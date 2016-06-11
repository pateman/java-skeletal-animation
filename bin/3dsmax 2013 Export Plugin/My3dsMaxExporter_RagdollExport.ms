dotNet.LoadAssembly "system.xml.dll"
dotNet.LoadAssembly "Newtonsoft.Json.dll"

-- Prepares the given value for storing in JSON.
fn getValueForJSON value = (
	local val = value
	if isProperty val "toJSON" then (
		val = value.toJSON()
	)
	val
)

-- Adds a new property to the given JSON container.
fn addJSONProperty &container key value = (
	local prop = dotNetObject "Newtonsoft.Json.Linq.JProperty" key (getValueForJSON value)
	container.Add prop
)

-- Converts an array to a JSON array.
fn arrayToJSON array = (
	local arr = dotNetObject "Newtonsoft.Json.Linq.JArray"
	for i = 1 to array.count do (
		arr.Add(getValueForJSON(array[i]))
	)
	arr
)

-- Converts a Matrix3 to JSON.
fn mat3ToJSON mat3 = (
	mat = #()
	mat[12]
	
	local cnt = 1
	for i = 1 to 4 do (
		mat[cnt] = mat3[i].x
		cnt += 1
		
		mat[cnt] = mat3[i].y
		cnt += 1
		
		mat[cnt] = mat3[i].z
		cnt += 1
	)
	
	arrayToJSON mat
)

struct RagdollRigidBody (
	name,
	vertices,
	triangles,
	matrix,
	fn toJSON = (
		local obj = dotNetObject "Newtonsoft.Json.Linq.JObject"
		
		-- Convert strings to floats.
		for i = 1 to vertices.count do (
			vertices[i] = vertices[i] as float
		)
		
		for i = 1 to triangles.count do (
			triangles[i] = triangles[i] as integer
		)
		
		addJSONProperty obj "name" name
		addJSONProperty obj "vertices" vertices
		addJSONProperty obj "triangles" triangles
		addJSONProperty obj "TM" (mat3ToJSON matrix)
		obj
	)
)

struct RagdollConstraint (
	firstBody,
	secondBody,
	swingYLocked,
	swingYAngle,
	swingZLocked,
	swingZAngle,
	twistLocked,
	twistLowAngle,
	twistHighAngle,
	firstBodyTM,
	secondBodyTM,
	fn toJSON = (
		local obj = dotNetObject "Newtonsoft.Json.Linq.JObject"
		
		addJSONProperty obj "bodies" (arrayToJSON(#(firstBody, secondBody)))
		addJSONProperty obj "locks" (arrayToJSON(#(swingYLocked, swingZLocked, twistLocked)))
		addJSONProperty obj "swingAngles" (arrayToJSON(#(swingYAngle, swingZAngle)))
		addJSONProperty obj "twistAngles" (arrayToJSON(#(twistLowAngle, twistHighAngle)))
		addJSONProperty obj "firstBodyTM" (mat3ToJSON(firstBodyTM))
		addJSONProperty obj "secondBodyTM" (mat3ToJSON(secondBodyTM))
		obj
	)
)

struct RagdollSkinData (
	public
	name,
	skinModifier,
	rigidbodies,
	constraints,
	private
	boneNames,
	fn initializeBoneNames = (
		if boneNames == undefined then (
			-- skinOps requires the skin to be selected in the "Modify" panel.
			max modify mode
			modPanel.setCurrentObject skinModifier
			
			boneNames = for i = 1 to (skinOps.GetNumberBones skinModifier) collect (skinOps.GetBoneName skinModifier i 0)
		)
	),
	public
	fn hasBone boneName = (
		-- Surely there's a better way to invoke initializeBoneNames(), but I'm just too lazy to work it out right now...
		initializeBoneNames()
		(findItem boneNames boneName) != 0
	),
	fn toJSON = (
		local obj = dotNetObject "Newtonsoft.Json.Linq.JObject"
		
		addJSONProperty obj "name" name
		addJSONProperty obj "rigidbodies" (arrayToJSON rigidbodies)
		addJSONProperty obj "constraints" (arrayToJSON constraints)
		obj
	)
)

fn getSkinForBone &skinsArray boneName = (
	for j = 1 to skinsArray.count do (
		local currentSkin = skinsArray[j]
		if (currentSkin.hasBone(boneName)) then (
			return currentSkin
		)
	)
	return undefined
)

fn processSkinModifier &skinsArray skinModifier skinNode = (
	local ragdollSkin = RagdollSkinData name: skinNode.name
	ragdollSkin.skinModifier = skinModifier
	ragdollSkin.rigidbodies = #()
	ragdollSkin.constraints = #()
		
	append skinsArray ragdollSkin
)

fn processRigidBody &skinsArray rigidBodyModifier rigidBodyNode = (
	local rigidBodyMesh = rigidBodyModifier.GetRBMeshShape 1
	local rigidBody = RagdollRigidBody name: rigidBodyNode.name

	local vertices = #()
	local faces = #()
					
	-- Initialize arrays.
	local vertexCount = getNumVerts rigidBodyMesh
	vertices[vertexCount * 3]
	
	local faceCount = getNumFaces rigidBodyMesh
	faces[faceCount * 3]
	
	-- Export vertices
	local cnt = 0
	for j = 1 to vertexCount do (
		local v = getVert rigidBodyMesh j
		vertices[cnt * 3 + 1] = v[1]
		vertices[cnt * 3 + 2] = v[2]
		vertices[cnt * 3 + 3] = v[3]
		
		cnt += 1
	)
	
	-- Export faces
	cnt = 0
	for j = 1 to faceCount do (
		local f = getFace rigidBodyMesh j
		faces[cnt * 3 + 1] = f[1] - 1
		faces[cnt * 3 + 2] = f[2] - 1
		faces[cnt * 3 + 3] = f[3] - 1
		
		cnt += 1
	)
	
	rigidBody.vertices = vertices
	rigidBody.triangles = faces
	rigidBody.matrix = rigidBodyNode.transform

	-- Figure out which skin the body belongs to.
	local rigidBodySkin = getSkinForBone skinsArray rigidBody.name
	if rigidBodySkin != undefined then (
		append rigidBodySkin.rigidbodies rigidBody
	)
)

fn processJoint &skinsArray jointNode = (
	local firstBody = jointNode.body0
	local secondBody = jointNode.body1
	
	local constraint = RagdollConstraint firstBody: firstBody.name secondBody: secondBody.name
	local constraintSkin = getSkinForBone skinsArray firstBody.name
	
	constraint.swingYLocked = jointNode.swing1Mode == 2
	constraint.swingYAngle = jointNode.swing1Angle
	constraint.swingZLocked = jointNode.swing2Mode == 2
	constraint.swingZAngle = jointNode.swing2Angle
	
	constraint.twistLocked = jointNode.twistMode == 2
	constraint.twistLowAngle = jointNode.twistAngleLow
	constraint.twistHighAngle = jointNode.twistAngleHigh
	
	constraint.firstBodyTM = firstBody.transform
	constraint.secondBodyTM = secondBody.transform
	
	if constraintSkin != undefined then (
		append constraintSkin.constraints constraint
	)
)

allObjects = $*
skins = #()
json = dotNetObject "Newtonsoft.Json.Linq.JObject"
for OBJ in allObjects do (			
	-- Check if we're dealing with a mesh object.
	if classof OBJ == Editable_mesh then (
		--	Process the modifiers.
		for i = 1 to OBJ.modifiers.count do (
			local currentModifier = OBJ.modifiers[i]
			local modifierClass = classof currentModifier
			
			-- Check if it has a Skin modifier.
			if modifierClass == Skin then (
				processSkinModifier skins currentModifier OBJ
				exit
			)
			
			-- Check if it has a MassFX_RBody modifier.
			if modifierClass == MassFX_RBody then (
				processRigidBody skins currentModifier OBJ 
			)
		)
		
		continue
	)
	
	-- Check if we're dealing with a joint.
	if classof OBJ == UConstraint then (
		processJoint skins OBJ
	)
)

-- Serialize everything to JSON.
converter = dotnetclass "Newtonsoft.Json.JsonConvert"
noFormatting = (dotnetclass "Newtonsoft.Json.Formatting").None
for i = 1 to skins.count do (
	local currentSkin = skins[i]
	local skinNode = getNodeByName currentSkin.name
	
	local jsonObject = getValueForJSON currentSkin
	local jsonString = converter.SerializeObject jsonObject noFormatting
	
	setUserProp skinNode "RAGDOLL_SKIN_DATA" jsonString
)
