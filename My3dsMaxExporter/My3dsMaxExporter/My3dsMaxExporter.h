#pragma once
#include "stdafx.h"
#include "NamedPipe.h"

struct BoneData {
	Matrix3 bindMatrix;
	Point3 bindPos;
};

class JSONExporter : public SceneExport {
	int	ExtCount();
	const TCHAR* Ext(int n);
	const TCHAR* LongDesc();
	const TCHAR* ShortDesc();
	const TCHAR* AuthorName();
	const TCHAR* CopyrightMessage();
	const TCHAR* OtherMessage1();
	const TCHAR* OtherMessage2();
	unsigned int Version();
	void ShowAbout(HWND hWnd);

	BOOL SupportsOptions(int ext, DWORD options);
	int	DoExport(const TCHAR *name, ExpInterface *ei, Interface *i, BOOL suppressPrompts = FALSE, DWORD options = 0);
private:
	IGameScene* scene;
	IFrameTagManager* frameTagManager;
	Interval animationRange;
	Tab<IGameNode*> bones;
	std::map<IGameNode*, BoneData> boneBindMatrices;
	std::map<INode*, Matrix3> globalNodeOffsetMatrices;
	float lengthUnitMultiplier;

	std::string prepareNodeNameForExport(const wchar_t* nodeName);

	void processAnimation(IGameNode* node, const std::vector<int> animationTimes, int ticksPerSecond, NamedPipe* pipe);
	void processMesh(IGameNode* node, NamedPipe* pipe);
	void processNode(IGameNode* node, Interface* coreInterface, NamedPipe* pipe);
	std::string matrixToString(const Matrix3 matrix);
	std::string transformToString(const Point3 translation, const Quat rotation, const Point3 scale);
};