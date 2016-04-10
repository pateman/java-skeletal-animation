#include "stdafx.h"
#include "NamedPipe.h"
#include "Utils.h"

NamedPipe::NamedPipe() {
	//	Compose the path to the pipe.
	std::string pipePath = "\\\\.\\pipe\\";
	pipePath.append(PIPE_NAME);

	this->pipeHandle = CreateNamedPipe(s2ws(pipePath).c_str(),
		PIPE_ACCESS_DUPLEX | PIPE_TYPE_BYTE | PIPE_READMODE_MESSAGE,
		PIPE_WAIT,
		1,
		1024 * 16,
		1024 * 16,
		NMPWAIT_USE_DEFAULT_WAIT,
		NULL);

	if (this->pipeHandle == INVALID_HANDLE_VALUE) {
		throw std::runtime_error("Unable to create a named pipe");
	}
};

NamedPipe::~NamedPipe() {
	if (this->pipeHandle != INVALID_HANDLE_VALUE) {
		CloseHandle(this->pipeHandle);
	}
};

bool NamedPipe::waitForConnection() {
	bool result = ConnectNamedPipe(this->pipeHandle, NULL);
	return result;
}

bool NamedPipe::writeToPipe(std::string message) {
	//	Terminate the message with a line ending if it's not present.
	if (*message.rbegin() != '\n') {
		message.append("\n");
	}

	//	Write to the pipe.
	return WriteFile(this->pipeHandle, message.c_str(), message.length(), NULL, NULL);
}