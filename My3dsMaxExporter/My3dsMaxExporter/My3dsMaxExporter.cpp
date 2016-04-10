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

int	JSONExporter::DoExport(const TCHAR *name, ExpInterface *ei, Interface *i, BOOL suppressPrompts, DWORD options) {
	DebugPrint(_T("Starting export"));

	//	TODO: Find a more elegant way to do that.
	std::wstring nameAsWString(name);
	std::string destinationPath(nameAsWString.begin(), nameAsWString.end());

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

		pipe->writeToPipe("test message");
		pipe->writeToPipe("END");
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