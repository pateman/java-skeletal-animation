#include "stdafx.h"
#include "My3dsMaxExporter.h"

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

int	JSONExporter::DoExport(const TCHAR *name, ExpInterface *ei, Interface *i, BOOL suppressPrompts, DWORD options) {
	return 1;
}