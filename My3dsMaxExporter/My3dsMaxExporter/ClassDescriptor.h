#pragma once

#include "stdafx.h"
#include "My3dsMaxExporter.h"

#define JSONEXPORTER_CLASS_ID Class_ID(0x10233c9d, 0x57d1ca0)

class JSONExporterClassDescriptor : public ClassDesc2 {
public:
	int IsPublic() { 
		return TRUE; 
	}
	void* Create(BOOL loading = FALSE) { 
		return new JSONExporter();
	}
	const TCHAR* ClassName() { 
		return _T("JSONExporter"); 
	}
	SClass_ID SuperClassID() { 
		return SCENE_EXPORT_CLASS_ID; 
	}
	Class_ID ClassID() { 
		return JSONEXPORTER_CLASS_ID;
	}
	const TCHAR* Category() { 
		return _T("Export"); 
	}
	const TCHAR* InternalName() { 
		return _T("JSONExporter"); 
	}
	HINSTANCE HInstance() { 
		return hInstance; 
	}
};

static JSONExporterClassDescriptor JSONExporterClassDesc;
ClassDesc2* GetJSONExporterClassDescriptor() {
	return &JSONExporterClassDesc;
}