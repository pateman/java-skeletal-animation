#pragma once

#include "targetver.h"
#include "Max.h"

#include "istdplug.h"
#include "iparamb2.h"
#include "iparamm2.h"
#include "decomp.h"

#include "IGame.h"
#include "IGameObject.h"
#include "IGameProperty.h"
#include "IGameControl.h"
#include "IGameModifier.h"
#include "IConversionManager.h"
#include "IFrameTagManager.h"
#include "IGameError.h"
#include "IGameFX.h"

#include "3dsmaxport.h"

#define WIN32_LEAN_AND_MEAN

#include <windows.h>
#include <string>
#include <exception>
#include <memory>
#include <vector>
#include <map>
#include <algorithm>

extern HINSTANCE hInstance;
