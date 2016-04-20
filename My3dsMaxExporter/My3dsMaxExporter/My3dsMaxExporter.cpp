#include "stdafx.h"
#include "My3dsMaxExporter.h"
#include "NamedPipe.h"
#include "Utils.h"

int JSONExporter::ExtCount() {
	return 1;
}

const TCHAR *JSONExporter::Ext(int n) {
	return _T("json");
}

const TCHAR *JSONExporter::LongDesc() {
	return _T("JSON Exporter for Java");
}

const TCHAR *JSONExporter::ShortDesc() {
	return _T("JSON Exporter");
}

const TCHAR *JSONExporter::AuthorName() {
	return _T("Patryk Nusbaum");
}

const TCHAR *JSONExporter::CopyrightMessage() {
	return _T("");
}

const TCHAR *JSONExporter::OtherMessage1() {
	return _T("");
}

const TCHAR *JSONExporter::OtherMessage2() {
	return _T("");
}

unsigned int JSONExporter::Version() {
	return 100;
}

void JSONExporter::ShowAbout(HWND hWnd) {
	//	Do nothing.
}

BOOL JSONExporter::SupportsOptions(int ext, DWORD options) {
	return TRUE;
}

//	Just a dummy function for the progress bar.
DWORD WINAPI progressBarDummy(LPVOID arg) {
	return 0;
}

int	JSONExporter::DoExport(const TCHAR *name, ExpInterface *ei, Interface *i, BOOL suppressPrompts, DWORD options) {
	DebugPrint(_T("Starting export"));

	//	TODO: Find a more elegant way to do that.
	std::string destinationPath = tchar2s(name);

	//	Create a named pipe.
	NamedPipe* pipe = new NamedPipe();
	try {
		//	Launch the Java client. Pass in the pipe's name and the destination file.
		std::string command = string_format("start java -jar \"%sMy3dsMaxExporter.jar\" %s \"%s\"", get3dsMaxPath().c_str(), PIPE_NAME, destinationPath.c_str());
		DebugPrint(s2ws(command).c_str());
		system(command.c_str());

		//	Wait for the client to connect to the pipe.
		if (!pipe->waitForConnection()) {
			throw std::runtime_error("Unable to establish connection with the client");
		}
		DebugPrint(_T("Client connected to the pipe"));

		//	Start processing the scene.
		Interface* coreInterface = GetCOREInterface();
		coreInterface->ProgressStart(_T("Exporting..."), TRUE, progressBarDummy, NULL);
		
		this->scene = GetIGameInterface();
		this->scene->InitialiseIGame(false);
		this->scene->SetStaticFrame(0);

		//	Retrieve the tag manager.
		this->frameTagManager = (IFrameTagManager*)GetCOREInterface(FRAMETAGMANAGER_INTERFACE);
		this->animationRange = coreInterface->GetAnimRange();

		//	Get the conversion manager and set the coordinate system to OpenGL.
		IGameConversionManager* conversionManager = GetConversionManager();
		conversionManager->SetCoordSystem(IGameConversionManager::CoordSystem::IGAME_OGL);

		//	Find all meshes in the scene.
		Tab<IGameNode*> meshes = this->scene->GetIGameNodeByType(IGameObject::ObjectTypes::IGAME_MESH);
		int meshCount = meshes.Count();

		//	Find all bones in the scene.
		this->bones = this->scene->GetIGameNodeByType(IGameObject::ObjectTypes::IGAME_BONE);

		for (int i = 0; i < meshCount; i++) {
			IGameNode* node = meshes[i];

			//	If the node is a target of another node, skip it.
			if (node->IsTarget()) {
				continue;
			}

			//	Update the progress bar.
			TSTR progress = _T("Processing ");
			progress += node->GetName();
			coreInterface->ProgressUpdate((int)((float)i / meshCount * 100.0f), FALSE, progress.data());

//	Process the node.
this->processNode(node, coreInterface, pipe);
		}

		//	Tell the Java client to close connection and release resources.
		pipe->writeToPipe("END");
		this->scene->ReleaseIGame();
		coreInterface->ProgressEnd();
	}
	catch (std::exception& e) {
		//	Print any exception.
		DebugPrint(_T("Exception thrown: '%s'", e.what()));
	}

	//	Finalize the export.
	delete pipe;
	DebugPrint(_T("Finishing export"));

	return 1;
}

std::string JSONExporter::prepareNodeNameForExport(const wchar_t* nodeName) {
	WStr newName = TSTR(nodeName);
	newName.Replace(_T(" "), _T("%%20"));

	return tchar2s(newName);
}

std::string JSONExporter::matrixToString(const Matrix3 matrix) {
	Point3 translation, scale;
	Quat rotation;

	//	Get translation, rotation, and scale from the transformation matrix.
	DecomposeMatrix(matrix, translation, rotation, scale);

	//	Write the decomposed parts of the matrix to the string.
	return string_format("%.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f",
		translation.x, translation.y, translation.z,
		rotation.x, rotation.y, rotation.z, rotation.w,
		scale.x, scale.y, scale.z);
}

void JSONExporter::processAnimation(const std::vector<int> animationTimes, int ticksPerSecond, NamedPipe* pipe) {
	int j;
	for (int i = 0; i < this->bones.Count(); i++) {
		IGameNode* bone = this->bones[i];

		pipe->writeToPipe(string_format("BEGIN_TRACK %d", bone->GetNodeID()));

		for (j = 0; j < animationTimes.size(); j++) {
			float keyframeTime = (float(animationTimes[j] - animationTimes[0]) / float(ticksPerSecond)) / GetFrameRate();

			Matrix3 keyframeMatrix = bone->GetLocalTM(animationTimes[j]).ExtractMatrix3();
			pipe->writeToPipe(string_format("KEYFRAME %.6f %s",
				keyframeTime,
				this->matrixToString(keyframeMatrix).c_str()
			));
		}

		pipe->writeToPipe("FINISH_TRACK");
	}
}

void JSONExporter::processMesh(IGameNode* node, NamedPipe* pipe) {
	IGameMesh* mesh = (IGameMesh*)node->GetIGameObject();
	IGameSkin* skin = mesh->GetIGameSkin();
	int counter, i, j;

	if (!mesh->InitializeData()) {
		DebugPrint(TSTR(_T("Unable to initialize mesh data for object ")).Append(node->GetName()));
		return;
	}

	//	Iterate over the available faces, and for each face, extract its geometry data.
	counter = mesh->GetNumberOfFaces();
	int vertexCount = 0;
	DWORD* mapIndices = new DWORD[3];

	for (i = 0; i < counter; i++) {
		FaceEx* face = mesh->GetFace(i);

		mesh->GetMapFaceIndex(1, face->meshFaceIndex, mapIndices);
		for (j = 0; j < 3; j++) {
			Point3 vertex, normal, uv;
			mesh->GetVertex(face->vert[j], vertex, true);
			mesh->GetNormal(face->norm[j], normal, true);
			mesh->GetMapVertex(1, mapIndices[j], uv);

			pipe->writeToPipe(string_format("VERTEX %.6f %.6f %.6f", vertex.x, vertex.y, vertex.z));
			pipe->writeToPipe(string_format("NORMAL %.6f %.6f %.6f", normal.x, normal.y, normal.z));
			pipe->writeToPipe(string_format("TEXCOORD %.6f %.6f", uv.x, -uv.y));
		}

		pipe->writeToPipe(string_format("FACE %d %d %d", vertexCount, vertexCount + 1, vertexCount + 2));
		vertexCount += 3;
	}

	//	If the mesh has skinning information...
	if (skin != NULL) {
		//	... export the bone structure first.
		counter = this->bones.Count();
		for (i = 0; i < counter; i++) {
			IGameNode* bone = this->bones[i];
			IGameNode* parent = bone->GetNodeParent();

			Matrix3 bindMatrix = bone->GetLocalTM(0).ExtractMatrix3();
			pipe->writeToPipe(string_format("BONE %s %d %d %s",
				this->prepareNodeNameForExport(bone->GetName()).c_str(),
				bone->GetNodeID(),
				parent == NULL ? -1 : parent->GetNodeID(),
				this->matrixToString(bindMatrix).c_str()
				));
		}

		//	We are using time tags to specify animations' times and names. In order to define an animation, you need to create
		//	two time tags - the first marks at which frame the animation starts and the second one (needs to be relative to the
		//	first one) indicates the end of the animation.
		counter = this->frameTagManager->GetTagCount();
		int nextTag;
		DWORD tagId;
		TimeValue timeStart, timeEnd;

		int ticksPerFrame = GetTicksPerFrame();
		std::vector<int> animationTimes;
		for (i = 0; i < counter; i++) {
			tagId = this->frameTagManager->GetTagID(i);

			//	This tag is locked to another tag. Ignore it, as it mostly likely indicates an animation's end.
			if (this->frameTagManager->GetLockIDByID(tagId) != 0) {
				continue;
			}

			//	Get the start and end of the animation.
			timeStart = this->frameTagManager->GetTimeByID(tagId, FALSE);
			timeEnd = this->animationRange.End();
			std::string name = tchar2s(this->frameTagManager->GetNameByID(tagId));
			
			nextTag = i + 1;
			if (nextTag < counter) {
				timeEnd = this->frameTagManager->GetTimeByID(this->frameTagManager->GetTagID(nextTag), FALSE);
			}

			//	Calculate the animation's times and its length.
			animationTimes.clear();
			for (j = timeStart; j < timeEnd; j += ticksPerFrame) {
				animationTimes.push_back(j);
			}

			animationTimes.push_back(timeEnd);
			animationTimes.erase(std::unique(animationTimes.begin(), animationTimes.end()), animationTimes.end());

			if (animationTimes.empty()) {
				DebugPrint(TSTR(_T("Invalid animation definition ")).Append(s2ws(name).c_str()));
				continue;
			}
			int animationTicks = animationTimes[animationTimes.size() - 1] - animationTimes[0];
			float animationLength = (float(animationTicks) / float(ticksPerFrame)) / GetFrameRate();

			//	Write the data to the pipe.
			pipe->writeToPipe(string_format("BEGIN_ANIMATION %s %d %d %.6f", 
				this->prepareNodeNameForExport(s2ws(name).c_str()).c_str(), 
				timeStart, 
				timeEnd,
				animationLength
			));

			//	Export the animation.
			this->processAnimation(animationTimes, ticksPerFrame, pipe);

			pipe->writeToPipe("FINISH_ANIMATION");
		}
	}
}

void JSONExporter::processNode(IGameNode* node, Interface* coreInterface, NamedPipe* pipe) {
	IGameObject* gameObject = node->GetIGameObject();
	IGameObject::ObjectTypes gameObjectType = gameObject->GetIGameType();

	//	Check whether the object type is supported.
	if (gameObjectType != IGameObject::ObjectTypes::IGAME_MESH) {
		DebugPrint(TSTR(_T("Ignoring ")).Append(node->GetName()).Append(_T(" because of unsupported object type")));
	} else {
		IGameNode* parent = node->GetNodeParent();
		Matrix3 transformMatrix = node->GetLocalTM().ExtractMatrix3();
		
		//	Write node information to the pipe.
		pipe->writeToPipe(string_format("BEGIN_NODE %s %d %d %s", 
			this->prepareNodeNameForExport(node->GetName()).c_str(),
			node->GetNodeID(),
			parent ? parent->GetNodeID() : -1,
			this->matrixToString(transformMatrix).c_str()
		));

		//	Process the node according to its type.
		this->processMesh(node, pipe);

		pipe->writeToPipe("FINISH_NODE");
	}
}