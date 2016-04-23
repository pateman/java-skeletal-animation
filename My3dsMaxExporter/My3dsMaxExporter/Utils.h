#pragma once

#include "stdafx.h"

//	Name of the environment path which will be used to get the 3dsmax's installation path.
#define PATH3DSMAX_ENV_VAR "ADSK_3DSMAX_x64_2013"

//	Units conversion.
#define M2MM 0.001f
#define M2CM 0.01f
#define M2M 1.0f
#define M2KM 1000.0f
#define M2IN 0.0254f
#define M2FT 0.3048f
#define M2ML 1609.344f

//	http://stackoverflow.com/a/8098080/759049
std::string string_format(const std::string fmt_str, ...);

//	http://stackoverflow.com/a/27296/759049
std::wstring s2ws(const std::string& s);

std::string tchar2s(const TCHAR* str);

//	http://stackoverflow.com/a/24315631/759049
void replaceAll(std::string &str, const std::string& from, const std::string& to);

std::string get3dsMaxPath();

float convertToMeter(int unitType);
Matrix3 transformMatrix(Matrix3 originalMatrix);
Matrix3 getGlobalNodeMatrix(INode *node, int time);
Matrix3 uniformMatrix(Matrix3 originalMatrix);
Matrix3 getLocalUniformMatrix(INode *node, Matrix3 offsetMatrix, int time);
Matrix3 getLocalUniformMatrix(INode *node, INode *parent, Matrix3 offsetMatrix, int time);
Matrix3 getRelativeMatrix(Matrix3 m1, Matrix3 m2);