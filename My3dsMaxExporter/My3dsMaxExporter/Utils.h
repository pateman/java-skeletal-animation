#pragma once

#include "stdafx.h"

//	Name of the environment path which will be used to get the 3dsmax's installation path.
#define PATH3DSMAX_ENV_VAR "ADSK_3DSMAX_x64_2013"

//	http://stackoverflow.com/a/8098080/759049
std::string string_format(const std::string fmt_str, ...);

//	http://stackoverflow.com/a/27296/759049
std::wstring s2ws(const std::string& s);

//	http://stackoverflow.com/a/24315631/759049
void replaceAll(std::string &str, const std::string& from, const std::string& to);

std::string get3dsMaxPath();