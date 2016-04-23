#include "stdafx.h"
#include "Utils.h"

std::string string_format(const std::string fmt_str, ...) {
	int final_n, n = ((int)fmt_str.size()) * 2; /* Reserve two times as much as the length of the fmt_str */
	std::string str;
	std::unique_ptr<char[]> formatted;
	va_list ap;
	while (1) {
		formatted.reset(new char[n]); /* Wrap the plain char array into the unique_ptr */
		strcpy(&formatted[0], fmt_str.c_str());
		va_start(ap, fmt_str);
		final_n = vsnprintf(&formatted[0], n, fmt_str.c_str(), ap);
		va_end(ap);
		if (final_n < 0 || final_n >= n)
			n += abs(final_n - n + 1);
		else
			break;
	}
	return std::string(formatted.get());
}

std::wstring s2ws(const std::string& s) {
	int len;
	int slength = (int)s.length() + 1;
	len = MultiByteToWideChar(CP_ACP, 0, s.c_str(), slength, 0, 0);
	wchar_t* buf = new wchar_t[len];
	MultiByteToWideChar(CP_ACP, 0, s.c_str(), slength, buf, len);
	std::wstring r(buf);
	delete[] buf;
	return r;
}

std::string tchar2s(const TCHAR* str) {
	std::wstring asWideString(str);
	std::string result(asWideString.begin(), asWideString.end());

	return result;
}

void replaceAll(std::string &str, const std::string& from, const std::string& to) {
	size_t start_pos = 0;
	while ((start_pos = str.find(from, start_pos)) != std::string::npos) {
		str.replace(start_pos, from.length(), to);
		start_pos += to.length(); // Handles case where 'to' is a substring of 'from'
	}
}

std::string get3dsMaxPath() {
	std::string path = getenv(PATH3DSMAX_ENV_VAR);
	replaceAll(path, "\\", "\\\\");
	return path;
}

float convertToMeter(int unitType) {
	float value = 1.0f;

	switch (unitType) {
	case UNITS_INCHES:
		value = M2IN;
		break;

	case UNITS_FEET:
		value = M2FT;
		break;

	case UNITS_MILES:
		value = M2ML;
		break;

	case UNITS_MILLIMETERS:
		value = M2MM;
		break;

	case UNITS_CENTIMETERS:
		value = M2CM;
		break;

	case UNITS_METERS:
		value = M2M;
		break;

	case UNITS_KILOMETERS:
		value = M2KM;
		break;
	}

	return value;
}

Matrix3 transformMatrix(Matrix3 originalMatrix) {
	Matrix3 YtoZ, ZtoY, mat;

	GMatrix gmat;
	gmat.SetRow(0, Point4(1, 0, 0, 0));
	gmat.SetRow(1, Point4(0, 0, 1, 0));
	gmat.SetRow(2, Point4(0, -1, 0, 0));
	gmat.SetRow(3, Point4(0, 0, 0, 1));
	YtoZ = gmat.ExtractMatrix3();
	ZtoY = Inverse(YtoZ);

	mat = YtoZ * originalMatrix * ZtoY;
	return mat;
}

Matrix3 getGlobalNodeMatrix(INode *node, int time) {
	return transformMatrix(node->GetNodeTM(time));
}

Matrix3 uniformMatrix(Matrix3 originalMatrix) {
	AffineParts parts;
	Matrix3 mat, YtoZ, ZtoY;

	GMatrix gmat;
	gmat.SetRow(0, Point4(1, 0, 0, 0));
	gmat.SetRow(1, Point4(0, 0, 1, 0));
	gmat.SetRow(2, Point4(0, -1, 0, 0));
	gmat.SetRow(3, Point4(0, 0, 0, 1));
	YtoZ = gmat.ExtractMatrix3();
	ZtoY = Inverse(YtoZ);

	//	Decompose the original matrix and get decomposition info.
	decomp_affine(originalMatrix, &parts);

	//	Construct a 3x3 rotation from the quaternion parts.q.
	parts.q.MakeMatrix(mat);

	//	Construct the position row from the translation parts.t.
	mat.SetRow(3, parts.t);

	//	We want to export using the Y axis as up.
	mat = YtoZ * mat * ZtoY;
	return mat;
}

Matrix3 getLocalUniformMatrix(INode *node, Matrix3 offsetMatrix, int time) {
	Matrix3 currentMatrix = uniformMatrix(node->GetNodeTM(time)) * Inverse(offsetMatrix);

	if (node->GetParentNode()->IsRootNode()) {
		return currentMatrix;
	}

	Matrix3 parentMatrix = uniformMatrix(node->GetParentNode()->GetNodeTM(time)) * Inverse(offsetMatrix);
	return currentMatrix * Inverse(parentMatrix);
}

Matrix3 getLocalUniformMatrix(INode *node, INode *parent, Matrix3 offsetMatrix, int time) {
	Matrix3 currentMatrix = uniformMatrix(node->GetNodeTM(time)) * Inverse(offsetMatrix);

	Matrix3 parentMatrix = transformMatrix(parent->GetNodeTM(time)) * Inverse(offsetMatrix);
	return currentMatrix * Inverse(parentMatrix);
}

Matrix3 getRelativeMatrix(Matrix3 m1, Matrix3 m2) {
	return m1 * Inverse(m2);
}