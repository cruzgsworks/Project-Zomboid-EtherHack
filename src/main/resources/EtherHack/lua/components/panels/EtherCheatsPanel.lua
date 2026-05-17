--***********************************************************
--**                    EtherHack Cheats Panel            **
--**            PZ 42.x DebugOptions API Implementation     **
--***********************************************************

require "ISUI/ISPanel"

-- Global cheat registry for easy access
EtherCheats = {}

-- Define all available cheats with their paths and display names
EtherCheats.categories = {
    {
        name = "Timed Actions",
        cheats = {
            { path = "cheat.timedAction.instant", label = "Instant Actions" }
        }
    },
    {
        name = "Player",
        cheats = {
            { path = "cheat.player.startInvisible", label = "Start Invisible" },
            { path = "cheat.player.invisibleSprint", label = "Invisible Sprint" },
            { path = "cheat.player.seeEveryone", label = "See Everyone" },
            { path = "cheat.player.unlimitedCondition", label = "Unlimited Condition" },
            { path = "cheat.player.fastLooseXp", label = "Fast XP Loss" }
        }
    },
    {
        name = "Vehicles",
        cheats = {
            { path = "cheat.vehicle.mechanicsAnywhere", label = "Mechanics Anywhere" },
            { path = "cheat.vehicle.startWithoutKey", label = "Start Without Key" }
        }
    },
    {
        name = "Environment",
        cheats = {
            { path = "cheat.clock.visible", label = "Clock Visible" },
            { path = "cheat.door.unlock", label = "Unlock Doors" },
            { path = "cheat.window.unlock", label = "Unlock Windows" }
        }
    },
    {
        name = "Farming",
        cheats = {
            { path = "cheat.farming.fastGrow", label = "Fast Grow" }
        }
    }
}

-- Helper function to toggle a cheat
function EtherCheats.toggleCheat(cheatPath)
    local debugOpts = getDebugOptions()
    local current = debugOpts:getBoolean(cheatPath)
    debugOpts:setBoolean(cheatPath, not current)
    return not current
end

-- Helper function to get cheat value
function EtherCheats.getCheatValue(cheatPath)
    return getDebugOptions():getBoolean(cheatPath)
end

-- Helper function to set cheat value
function EtherCheats.setCheatValue(cheatPath, value)
    getDebugOptions():setBoolean(cheatPath, value)
end

-- Helper function to enable all cheats
function EtherCheats.enableAllCheats()
    for _, category in ipairs(EtherCheats.categories) do
        for _, cheat in ipairs(category.cheats) do
            EtherCheats.setCheatValue(cheat.path, true)
        end
    end
end

-- Helper function to disable all cheats
function EtherCheats.disableAllCheats()
    for _, category in ipairs(EtherCheats.categories) do
        for _, cheat in ipairs(category.cheats) do
            EtherCheats.setCheatValue(cheat.path, false)
        end
    end
end

--***********************************************************
--**                Cheats Panel UI Class                  **
--***********************************************************

ISCheatsPanel = ISPanel:derive("ISCheatsPanel")
ISCheatsPanel.instance = nil

function ISCheatsPanel:initialise()
    ISPanel.initialise(self)
end

function ISCheatsPanel:createChildren()
    ISPanel.createChildren(self)
    
    local y = 10
    local x = 10
    local categorySpacing = 15
    
    self.checkboxes = {}
    
    -- Create Enable All / Disable All buttons
    local buttonWidth = (self.width - 30) / 2
    
    local enableAllBtn = ISButton:new(x, y, buttonWidth, 25, "Enable All", self, ISCheatsPanel.onEnableAll)
    enableAllBtn:initialise()
    enableAllBtn:instantiate()
    self:addChild(enableAllBtn)
    
    local disableAllBtn = ISButton:new(x + buttonWidth + 10, y, buttonWidth, 25, "Disable All", self, ISCheatsPanel.onDisableAll)
    disableAllBtn:initialise()
    disableAllBtn:instantiate()
    self:addChild(disableAllBtn)
    
    y = y + 35
    
    -- Add category sections
    for _, category in ipairs(EtherCheats.categories) do
        -- Category header
        local header = ISLabel:new(x, y, 20, category.name, 1, 1, 0.5, 1, UIFont.Medium, true)
        header:initialise()
        self:addChild(header)
        y = y + 25
        
        -- Add cheats for this category
        for _, cheat in ipairs(category.cheats) do
            -- Label
            local label = ISLabel:new(x + 10, y + 3, 16, cheat.label, 1, 1, 1, 1, UIFont.Small, true)
            label:initialise()
            self:addChild(label)
            
            -- Tickbox
            local tickbox = ISTickBox:new(self.width - 120, y, 100, 20, "")
            tickbox:initialise()
            tickbox:instantiate()
            tickbox:addOption("", cheat.path)
            tickbox:setSelected(1, EtherCheats.getCheatValue(cheat.path))
            tickbox.changeOptionMethod = function(target, index, selected)
                EtherCheats.setCheatValue(cheat.path, selected)
            end
            tickbox.changeOptionTarget = tickbox
            self:addChild(tickbox)
            
            -- Store reference for updates
            table.insert(self.checkboxes, {
                tickbox = tickbox,
                path = cheat.path
            })
            
            y = y + 22
        end
        
        -- Add spacing between categories
        y = y + categorySpacing
    end
    
    self:setScrollHeight(y + 10)
end

function ISCheatsPanel:onEnableAll()
    EtherCheats.enableAllCheats()
    self:updateCheckboxes()
end

function ISCheatsPanel:onDisableAll()
    EtherCheats.disableAllCheats()
    self:updateCheckboxes()
end

function ISCheatsPanel:updateCheckboxes()
    for _, checkboxData in ipairs(self.checkboxes) do
        local currentValue = EtherCheats.getCheatValue(checkboxData.path)
        checkboxData.tickbox:setSelected(1, currentValue)
    end
end

function ISCheatsPanel:update()
    ISPanel.update(self)
    -- Refresh checkbox states in case they were changed externally
    if self.checkboxes then
        self:updateCheckboxes()
    end
end

function ISCheatsPanel:new(x, y, width, height)
    local o = ISPanel:new(x, y, width, height)
    setmetatable(o, self)
    self.__index = self
    o.backgroundColor = {r=0, g=0, b=0, a=0.8}
    o.borderColor = {r=1, g=1, b=1, a=0.3}
    return o
end

-- Static open method
function ISCheatsPanel.OnOpenPanel()
    if ISCheatsPanel.instance then
        ISCheatsPanel.instance:close()
        ISCheatsPanel.instance = nil
        return
    end
    
    local width = 400
    local height = 600
    local x = getCore():getScreenWidth() / 2 - width / 2
    local y = getCore():getScreenHeight() / 2 - height / 2
    
    local panel = ISCheatsPanel:new(x, y, width, height)
    panel:initialise()
    panel:addToUIManager()
    ISCheatsPanel.instance = panel
    
    return panel
end

-- Function to add cheats panel button to EtherDebugMenu
function EtherCheats.addToDebugMenu(debugMenu)
    debugMenu:addButtonInfo("Cheats", function() ISCheatsPanel.OnOpenPanel() end, "MAIN")
end

print("[EtherHack]: Cheats panel loaded")
