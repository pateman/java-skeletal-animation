#include "stdafx.h"
#include "ClassDescriptor.h"

HINSTANCE hInstance;

BOOL APIENTRY DllMain(HMODULE hModule, DWORD ul_reason_for_call, LPVOID lpReserved) {
	if (ul_reason_for_call == DLL_PROCESS_ATTACH) {
		MaxSDK::Util::UseLanguagePackLocale();
		hInstance = hModule;
		DisableThreadLibraryCalls(hInstance);
	}

	return TRUE;
}

__declspec(dllexport) const TCHAR* LibDescription() {
	return _T("JSON Exporter");
}

__declspec(dllexport) int LibNumberClasses() {
	return 1;
}

__declspec(dllexport) ClassDesc* LibClassDesc(int i) {
	switch (i) {
		case 0: 
			return GetJSONExporterClassDescriptor();
		default: 
			return 0;
	}
}

__declspec(dllexport) ULONG LibVersion() {
	return VERSION_3DSMAX;
}

__declspec(dllexport) ULONG CanAutoDefer() {
	return 1;
}

