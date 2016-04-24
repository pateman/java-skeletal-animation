anims = #()

struct AnimDef(
	Start,
	End,
	Name
)

--get a list of animations from the text file
fn GetAnimList animFilename = (
	local file = memStreamMgr.openFile animFilename

	while NOT file.eos() do (
		local line = filterString (file.readLine()) " "
		if line.count == 4  and line[2]=="-" then (
			local anim = AnimDef (line[1] as Integer) (line[3] as Integer) line[4]
			append Anims anim
		)
	)
	memStreamMgr.close file
)

animFilename = getOpenFileName caption:"Select animation list" types:"Text File (*.txt)|*.txt|"
GetAnimList animFilename

FrameTagManager.ResetFrameTags() 
for i = 1 to anims.Count do(
	local startTag = FrameTagManager.CreateNewTag anims[i].Name anims[i].Start
	local endTag = FrameTagManager.CreateNewTag anims[i].Name anims[i].End
	
	FrameTagManager.SetLockIDByID endTag startTag
)