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

struct RagdollRigidBody (
	name,
	vertices,
	triangles,
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
		obj
	)
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
		local rigidbodies = #()
		
		-- Find rigidbodies and dump their meshes. This script assumes that all rigid bodies are made of convex hulls.
		local xmlRigidbodies = xmlDoc.SelectNodes "//rigidbody"
		for i = 0 to (xmlRigidbodies.Count - 1) do (
			local xmlRigidBody = xmlRigidbodies.ItemOf i
			
			local meshNode = xmlRigidBody.SelectSingleNode "mesh"			
			local vertices = FilterString (meshNode.SelectSingleNode("vertices")).InnerText ","
			local faces = FilterString (meshNode.SelectSingleNode("faces")).InnerText ","
			
			local rigidBody = RagdollRigidBody name: (xmlRigidBody.GetAttribute "name") vertices: vertices triangles: faces
			append rigidbodies rigidBody
		)
		addJSONProperty json "rigidbodies" (arrayToJSON rigidbodies)
		
		-- Find the skin.
		local skins = xmlDoc.SelectNodes "*/parameter[@name='multiRagdollSkins']"
		for i = 0 to (skins.Count - 1) do (
			local skin = skins.ItemOf i
			local skinNode = getNodeByName skin.InnerText
			
			addJSONProperty json "skinObject" skin.InnerText			
		)
		
		local jsonString = json.ToString()
		print jsonString
		
		--deleteFile filename
	)
)

