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

-- Converts a Point3 to JSON.
fn point3ToJSON pt = (
	arrayToJSON(#(pt.x, pt.y, pt.z))
)

-- Converts a quaternion to JSON.
fn quatToJSON quat = (
	arrayToJSON(#(quat.x, quat.y, quat.z, quat.w))
)

struct RagdollRigidBody (
	name,
	vertices,
	triangles,
	position,
	rotation,
	scale,
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
		addJSONProperty obj "pos" (point3ToJSON position)
		addJSONProperty obj "rot" (quatToJSON rotation)
		addJSONProperty obj "scale" (point3ToJSON scale)
		obj
	)
)

struct RagdollConstraint (
	firstBody,
	secondBody,
	fn toJSON = (
		local obj = dotNetObject "Newtonsoft.Json.Linq.JObject"
		
		addJSONProperty obj "bodyA" firstBody
		addJSONProperty obj "bodyB" secondBody
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

fn getSkinForBone skinsArray boneName = (
	for j = 1 to skinsArray.count do (
		local currentSkin = skinsArray[j]
		if (currentSkin.hasBone(boneName)) then (
			return currentSkin
		)
	)
	return undefined
)

allObjects = $*
for OBJ in allObjects do(	
	local filename = "C:\\Users\\pateman\\Desktop\\" + OBJ.name + "test.xml"
	
	-- Hack to get the ragdoll's data. We're dumping the data to a file, and then parse it to
	-- our internal structures. Once we're done, we delete the file.
	if nvpx.RagdollSave OBJ filename do (	
		local xmlDoc = dotNetObject "System.Xml.XmlDocument" 
		xmlDoc.Load filename
		
		local json = dotNetObject "Newtonsoft.Json.Linq.JObject"
		local skins = #()
		
		-- Find the skins.
		local xmlSkins = xmlDoc.SelectNodes "*/parameter[@name='multiRagdollSkins']"
		for i = 0 to (xmlSkins.Count - 1) do (
			local xmlSkin = xmlSkins.ItemOf i
			local skinNode = getNodeByName xmlSkin.InnerText
			
			-- Find the 'Skin' modifier.
			for j = 1 to skinNode.modifiers.count do (
				if classof skinNode.modifiers[j] as string == "Skin" then (
					local ragdollSkin = RagdollSkinData name: skinNode.name
					ragdollSkin.skinModifier = skinNode.modifiers[j]
					ragdollSkin.rigidbodies = #()
					ragdollSkin.constraints = #()
					
					append skins ragdollSkin
				)
			)
		)
				
		-- Find rigidbodies and dump their meshes. This script assumes that all rigid bodies are made of convex hulls.
		local xmlRigidbodies = xmlDoc.SelectNodes "//rigidbody"
		for i = 0 to (xmlRigidbodies.Count - 1) do (
			local xmlRigidBody = xmlRigidbodies.ItemOf i
			local name = xmlRigidBody.GetAttribute "name"
			local bodyNode = getNodeByName name
						
			local meshNode = xmlRigidBody.SelectSingleNode "mesh"			
			local vertices = FilterString (meshNode.SelectSingleNode("vertices")).InnerText ","
			local faces = FilterString (meshNode.SelectSingleNode("faces")).InnerText ","
			
			local rigidBody = RagdollRigidBody name: name vertices: vertices triangles: faces
			rigidBody.position = bodyNode.position
			rigidBody.rotation = bodyNode.rotation
			rigidBody.scale = bodyNode.scale
			
			-- Figure out which skin the body belongs to.
			local rigidBodySkin = getSkinForBone skins rigidBody.name
			if rigidBodySkin != undefined then (
				append rigidBodySkin.rigidbodies rigidBody
			)
		)
		
		-- Find joints.
		local xmlJoints = xmlDoc.SelectNodes "//joint"
		for i = 0 to (xmlJoints.Count - 1) do (
			local xmlJoint = xmlJoints.ItemOf i
			
			local tmpNode = xmlJoint.SelectSingleNode "*[@name='body0']"
			local firstBody = tmpNode.InnerText
			
			tmpNode = xmlJoint.SelectSingleNode "*[@name='body1']"
			local secondBody = tmpNode.InnerText
			
			local constraint = RagdollConstraint firstBody: firstBody secondBody: secondBody
			local constraintSkin = getSkinForBone skins firstBody
			if constraintSkin != undefined then (
				append constraintSkin.constraints constraint
			)
		)
		
		-- Serialize everything to JSON.
		addJSONProperty json "skins" (arrayToJSON skins)
		local jsonString = json.ToString()
		print jsonString
		
		--deleteFile filename
	)
)

