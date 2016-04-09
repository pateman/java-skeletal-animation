#include "stdafx.h"

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
};