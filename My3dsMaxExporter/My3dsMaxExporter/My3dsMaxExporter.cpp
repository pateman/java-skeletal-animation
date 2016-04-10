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

		//	Get the conversion manager and set the coordinate system to OpenGL.
		IGameConversionManager* conversionManager = GetConversionManager();
		conversionManager->SetCoordSystem(IGameConversionManager::CoordSystem::IGAME_OGL);

		int topLevelNodes = this->scene->GetTopLevelNodeCount();
		for (int i = 0; i < topLevelNodes; i++) {
			IGameNode* node = this->scene->GetTopLevelNode(i);

			//	If the node is a target of another node, skip it.
			if (node->IsTarget()) {
				continue;
			}

			//	Update the progress bar.
			TSTR progress = _T("Processing ");
			progress += node->GetName();
			coreInterface->ProgressUpdate((int)((float)i / topLevelNodes * 100.0f), FALSE, progress.data());

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

void JSONExporter::writeMatrix(const Matrix3 matrix, NamedPipe* pipe) {
	Point3 translation, scale;
	Quat rotation;

	//	Get translation, rotation, and scale from the transformation matrix.
	DecomposeMatrix(matrix, translation, rotation, scale);

	//	Write the decomposed parts of the matrix to the pipe.
	pipe->writeToPipe(string_format("TRANSLATION %.6f %.6f %.6f", translation.x, translation.y, translation.z));
	pipe->writeToPipe(string_format("ROTATION %.6f %.6f %.6f %.6f", rotation.x, rotation.y, rotation.z, rotation.w));
	pipe->writeToPipe(string_format("SCALE %.6f %.6f %.6f", scale.x, scale.y, scale.z));
}

void JSONExporter::processNode(IGameNode* node, Interface* coreInterface, NamedPipe* pipe) {
	IGameObject* gameObject = node->GetIGameObject();
	IGameObject::ObjectTypes gameObjectType = gameObject->GetIGameType();

	//	Check whether the object type is supported.
	if (gameObjectType != IGameObject::ObjectTypes::IGAME_BONE && gameObjectType != IGameObject::ObjectTypes::IGAME_MESH) {
		DebugPrint(TSTR(_T("Ignoring ")).Append(node->GetName()).Append(_T(" because of unsupported object type")));
	} else {
		pipe->writeToPipe("BEGIN_NODE");

		//	Basic node data.
		pipe->writeToPipe(string_format("NAME %s", tchar2s(node->GetName()).c_str()));
		pipe->writeToPipe(string_format("INDEX %d", node->GetNodeID()));

		IGameNode* parent = node->GetNodeParent();
		pipe->writeToPipe(string_format("PARENT %d", parent ? parent->GetNodeID() : -1));

		pipe->writeToPipe(string_format("TYPE %s", gameObjectType != IGameObject::ObjectTypes::IGAME_MESH ? "Bone" : "Mesh"));
		
		//	Transformation matrix (in world space.)
		Matrix3 transformMatrix = node->GetWorldTM().ExtractMatrix3();
		this->writeMatrix(transformMatrix, pipe);

		pipe->writeToPipe("FINISH_NODE");
	}

	//	Process children.
	for (int kid = 0; kid < node->GetChildCount(); kid++) {
		this->processNode(node->GetNodeChild(kid), coreInterface, pipe);
	}
}