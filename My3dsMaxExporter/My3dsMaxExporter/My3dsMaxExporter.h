#pragma once
#include "stdafx.h"
#include "NamedPipe.h"

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

	std::string prepareNodeNameForExport(const wchar_t* nodeName);

	void processAnimation(const std::vector<int> animationTimes, int ticksPerSecond, NamedPipe* pipe);
	void processMesh(IGameNode* node, NamedPipe* pipe);
	void processNode(IGameNode* node, Interface* coreInterface, NamedPipe* pipe);
	std::string matrixToString(const Matrix3 matrix);
};