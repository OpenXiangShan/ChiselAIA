-- TestZhuJiang/src/test/lua/common/TileLink.lua
local utils = require "LuaUtils"

local enum_search = utils.enum_search

local function is_acquire(opcode)
	return opcode == 6 or opcode == 7
end

local function is_grant(opcode)
	return opcode == 4 or opcode == 5
end

local function is_accessack(opcode)
	return opcode == 0 or opcode == 1
end

local function is_probeack(opcode)
	return opcode == 4 or opcode == 5
end

local function is_release(opcode)
	return opcode == 6 or opcode == 7
end

local function is_releaseack(opcode)
	return opcode == 6
end

local function channel_c_has_data(opcode)
	return opcode == 5 or opcode == 7
end

local function channel_d_has_data(opcode)
	return opcode == 5 or opcode == 1
end

local Direction = setmetatable({
	name = "Direction",
	INPUT = 0,
	OUTPUT = 1,
}, { __call = enum_search })

local TLMessageA = setmetatable({
	name = "TLMessageA",
	PutFullData = 0,
	PutPartialData = 1,
	Get = 4,
	AcquireBlock = 6,
	AcquirePerm = 7,
}, { __call = enum_search })

local TLMessageB = setmetatable({
	name = "TLMessageB",
	Probe = 6,
}, { __call = enum_search })

local TLMessageC = setmetatable({
	name = "TLMessageC",
	ProbeAck = 4,
	ProbeAckData = 5,
	Release = 6,
	ReleaseData = 7,
}, { __call = enum_search })

local TLMessageD = setmetatable({
	name = "TLMessageD",
	AccessAck = 0,
	AccessAckData = 1,
	HintAck = 2,
	Grant = 4,
	GrantData = 5,
	ReleaseAck = 6,
}, { __call = enum_search })

local TLMessageE = setmetatable({
	name = "TLMessageE",
	GrantAck = 0,
}, { __call = enum_search })

local function opcode_to_str(channel, opcode)
	local tab = {
		["A"] = TLMessageA,
		["B"] = TLMessageB,
		["C"] = TLMessageC,
		["D"] = TLMessageD,
		["E"] = TLMessageE,
	}

	return tab[channel](opcode)
end

local TLParamCap = setmetatable({
	name = "TLParamCap",
	toT = 0,
	toB = 1,
	toN = 2,

	TtoT = 3,
	BtoB = 4,
	NtoN = 5,
}, { __call = enum_search })

local TLParamGrow = setmetatable({
	name = "TLParamGrow",
	NtoB = 0,
	NtoT = 1,
	BtoT = 2,

	TtoT = 3,
	BtoB = 4,
	NtoN = 5,
}, { __call = enum_search })

local TLParamShrink = setmetatable({
	name = "TLParamShrink",
	TtoB = 0,
	TtoN = 1,
	BtoN = 2,

	TtoT = 3,
	BtoB = 4,
	NtoN = 5,
	is_toN = function(param)
		return param == 1 or param == 2 or param == 5
	end,
}, { __call = enum_search })

local TileLinkMessage = {}

function TileLinkMessage:new(address, opcode, param, source, sink, data, mask, beat)
	local obj = {}

	obj.address = address
	obj.opcode = opcode
	obj.param = param
	obj.source = source
	obj.sink = sink
	obj.data = data
	obj.mask = mask or 2 ^ 32 - 1 -- TODO: parameterize
	obj.beat = beat

	return obj
end

local TileLinkTrans = {}

function TileLinkTrans:new(cycles, core, slice, level, channel, opcode, param, addr, source, sink, data, others)
	local obj = {}

	obj.cycles = cycles
	obj.core = core
	obj.slice = slice
	obj.level = level
	obj.channel = channel
	obj.opcode = opcode
	obj.param = param
	obj.addr = type(addr) == "table" and utils.serialize(addr) or tostring(addr)
	obj.source = source
	obj.sink = sink
	obj.data = type(data) == "table" and utils.serialize(data) or tostring(data)
	obj.others = others

	return obj
end

return {
	is_acquire = is_acquire,
	is_grant = is_grant,
	is_accessack = is_accessack,
	is_probeack = is_probeack,
	is_release = is_release,
	is_releaseack = is_releaseack,
	channel_c_has_data = channel_c_has_data,
	channel_d_has_data = channel_d_has_data,
	Direction = Direction,
	TLMessageA = TLMessageA,
	TLMessageB = TLMessageB,
	TLMessageC = TLMessageC,
	TLMessageD = TLMessageD,
	TLMessageE = TLMessageE,
	TLParamCap = TLParamCap,
	TLParamGrow = TLParamGrow,
	TLParamShrink = TLParamShrink,
	TileLinkMessage = TileLinkMessage,
	opcode_to_str = opcode_to_str,
	TileLinkTrans = TileLinkTrans,
}
