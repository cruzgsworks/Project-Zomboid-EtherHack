
require "ISUI/ISPanel"

--*********************************************************
--* Глобальные установки UI
--*********************************************************
UIModalAddTrait = ISPanel:derive("UIModalAddTrait");

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
UIModalAddTrait.negativeTraits = {
    -- Internal names
    ["Agoraphobic"] = true,
    ["AllThumbs"] = true,
    ["Asthmatic"] = true,
    ["Claustrophobic"] = true,
    ["Clumsy"] = true,
    ["Conspicuous"] = true,
    ["Cowardly"] = true,
    ["Deaf"] = true,
    ["Disorganized"] = true,
    ["Emaciated"] = true,
    ["Feeble"] = true,
    ["HardOfHearing"] = true,
    ["HeartyAppetite"] = true,
    ["Hemophobic"] = true,
    ["HighThirst"] = true,
    ["Illiterate"] = true,
    ["Insomniac"] = true,
    ["NeedsMoreSleep"] = true,
    ["Obese"] = true,
    ["Out of Shape"] = true,
    ["Overweight"] = true,
    ["Pacifist"] = true,
    ["ProneToIllness"] = true,
    ["ShortSighted"] = true,
    ["SlowHealer"] = true,
    ["SlowLearner"] = true,
    ["SlowReader"] = true,
    ["Smoker"] = true,
    ["SundayDriver"] = true,
    ["ThinSkinned"] = true,
    ["Underweight"] = true,
    ["Unfit"] = true,
    ["Very Underweight"] = true,
    ["Weak"] = true,
    ["WeakStomach"] = true,
    ["WeightGain"] = true,
    ["WeightLoss"] = true,
    
    -- PZ 42.x display names (weight traits)
    ["Very High Weight"] = true,
    ["High Weight"] = true,
    ["Low Weight"] = true,
    ["Very Low Weight"] = true,
}

function UIModalAddTrait:isNegativeTrait(traitDef)
    local traitName = traitDef:getType():getName()
    local traitLabel = traitDef:getLabel()
    
    -- Check internal name
    if UIModalAddTrait.negativeTraits[traitName] then
        return true
    end
    
    -- Check display label (PZ 42.x may use different display names)
    if UIModalAddTrait.negativeTraits[traitLabel] then
        return true
    end
    
    -- Fallback: check cost (negative cost = negative trait)
    if traitDef:getCost() < 0 then
        return true
    end
    return false
end

function UIModalAddTrait:createChildren()
    -- PZ 42.x: Use CharacterTraitDefinition.characterTraitDefinitions
    local allTraitDefs = CharacterTraitDefinition.getTraits()
    -- Get player's current traits
    local playerTraits = {}
    if self.localPlayer.getCharacterTraits then
        local charTraits = self.localPlayer:getCharacterTraits()
        if charTraits then
            local knownTraits = charTraits:getKnownTraits()
            if knownTraits then
                for i=0, knownTraits:size()-1 do
                    playerTraits[knownTraits:get(i)] = true
                end
            end
        end
    elseif self.localPlayer.getTraits then
        local traits = self.localPlayer:getTraits()
        if traits then
            for i=0, traits:size()-1 do
                playerTraits[traits:get(i)] = true
            end
        end
    end
    
    for i=0, allTraitDefs:size()-1 do
        local traitDef = allTraitDefs:get(i);
        local trait = traitDef:getType()
        if not playerTraits[trait] then
            if self:isNegativeTrait(traitDef) then
                table.insert(self.badTraits, traitDef)
            else
                table.insert(self.goodTraits, traitDef)
            end
        end
    end

    self.acceptButton = UIButton:new(10, self.height - 35, 100, 25, getTranslate("UI_PlayerEditor_PlayerTraits_ModalAccept"), 
    function() 
        UIModalAddTrait.instance:setVisible(false);
        UIModalAddTrait.instance:removeFromUIManager();
        UIModalAddTrait.instance = nil;

        local list = self.badTraits;
        if self.traitsSelector.isChecked then
            list = self.goodTraits;
        end
        local traitDef = list[self.combo.selected];
        local trait = traitDef:getType()
        
        -- PZ 42.x: Use CharacterTraits
        if self.localPlayer.getCharacterTraits then
            local charTraits = self.localPlayer:getCharacterTraits()
            if charTraits then
                charTraits:add(trait)
            end
        elseif self.localPlayer.getTraits then
            self.localPlayer:getTraits():add(trait:getName())
        end
        
        SyncXp(self.localPlayer);
        UITraitsTable.instance:updateTraits();
    end)
    self.acceptButton:initialise();
    self.acceptButton:instantiate();
    self.acceptButton:setAnchorLeft(true);
    self.acceptButton:setAnchorRight(false);
    self.acceptButton:setAnchorTop(false);
    self.acceptButton:setAnchorBottom(true);
    self.acceptButton.isOnlyInGame = true;
    self:addChild(self.acceptButton);
    table.insert(self.buttonList, self.acceptButton);

    self.closeButton = UIButton:new(self.acceptButton.x + self.acceptButton.width + 10, self.height - 35, 100, 25, getTranslate("UI_PlayerEditor_PlayerTraits_ModalClose"), 
    function() 
        UIModalAddTrait.instance:setVisible(false);
        UIModalAddTrait.instance:removeFromUIManager();
        UIModalAddTrait.instance = nil;
    end)
    self.closeButton:initialise();
    self.closeButton:instantiate();
    self.closeButton:setAnchorLeft(true);
    self.closeButton:setAnchorRight(false);
    self.closeButton:setAnchorTop(false);
    self.closeButton:setAnchorBottom(true);
    self.closeButton.isOnlyInGame = true;
    self:addChild(self.closeButton);
    table.insert(self.buttonList, self.closeButton);

    self.combo = ISComboBox:new(10, 10, self.width - 20, 30, nil,nil);
    self.combo:initialise();
    self.goodTrait = {};
    self:addChild(self.combo);

    self.traitsSelector = UICheckbox:new(10, self.combo.y + self.combo.height + 10, getTranslate("UI_PlayerEditor_PlayerTraits_IsGoodTrait"), true, function ()
        self:updateTraitsList();
    end)
    self.traitsSelector:initialise();
    self.traitsSelector:instantiate();
    self:addChild(self.traitsSelector);

    self:updateTraitsList();
end

--*********************************************************
--* Обновление черт характера
--*********************************************************
function UIModalAddTrait:updateTraitsList()
    self.combo:clear();
    local list = self.badTraits;
    if self.traitsSelector.isChecked then
        list = self.goodTraits;
    end
    local tooltipMap = {};
    for _,v in ipairs(list) do
        self.combo:addOption(v:getLabel());
        tooltipMap[v:getLabel()] = v:getDescription();
    end
    self.combo:setToolTipMap(tooltipMap);

    if self.traitsSelector.isChecked then
        local hc = getCore():getGoodHighlitedColor()
        self.combo.textColor = {r=hc:getR(), g=hc:getG(), b=hc:getB(),a=0.9};
    else
        local hc = getCore():getBadHighlitedColor()
        self.combo.textColor = {r=hc:getR(), g=hc:getG(), b=hc:getB(),a=0.9};
    end
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function UIModalAddTrait:new()
    local menuTableData = {};

    local width, height = 230, 110;
    local positionX = getCore():getScreenWidth() / 2 - width / 2;
    local positionY = getCore():getScreenHeight() / 2 - height/ 2;

    menuTableData = ISPanel:new(positionX, positionY, width, height);
    setmetatable(menuTableData, self);
    self.__index = self;
    menuTableData.variableColor={r=0.9, g=0.55, b=0.1, a=1};
    menuTableData.borderColor = {r=0.4, g=0.4, b=0.4, a=1};
    menuTableData.backgroundColor = {r=0, g=0, b=0, a=0.8};
    menuTableData.localPlayer = getPlayer();
    menuTableData.comboList = {};
    menuTableData.goodTraits = {};
    menuTableData.badTraits = {};
    menuTableData.buttonList = {};
    menuTableData.moveWithMouse = true;
    UIModalAddTrait.instance = menuTableData;
    return menuTableData;
end
