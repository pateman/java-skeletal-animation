#pragma once

#include "stdafx.h"
#define PIPE_NAME "My3dsMaxExporter"

class NamedPipe {
public:
	NamedPipe();
	~NamedPipe();

	bool waitForConnection();
	bool writeToPipe(std::string message);
private:
	HANDLE pipeHandle;
};