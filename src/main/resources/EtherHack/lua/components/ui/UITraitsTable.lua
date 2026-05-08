require "ISUI/ISPanel"

--*********************************************************
--* Глобальные установки UI
--*********************************************************
UITraitsTable = ISPanel:derive("UITraitsTable");

local fontHeightSmall = getTextManager():getFontHeight(UIFont.Small)

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function UITraitsTable:createChildren()
    ISPanel.createChildren(self);

    self.datas = ISScrollingListBox:new(0, 0, self.width, self.height - 90);
    self.datas:initialise();
    self.datas:instantiate();
    self.datas.itemheight = fontHeightSmall + 4 * 2
    self.datas.selected = 0;
    self.datas.joypadParent = self;
    self.datas.font = UIFont.NewSmall;
    self.datas.doDrawItem = self.drawDatas;
    self.datas.drawBorder = true;
    self.datas.backgroundColor = {r=0, g=0, b=0, a=0.0};
    self.datas:addColumn(getTranslate("UI_PlayerEditor_PlayerTraits_NameTitle"), 0);
    self.datas:addColumn(getTranslate("UI_PlayerEditor_PlayerTraits_Description"), 150)
    self:addChild(self.datas);


    self.addTrait = UIButton:new(0, self.height - 80, 100, 24, getTranslate("UI_PlayerEditor_PlayerTraits_AddTrait"), 
    function() 
        if UIModalAddTrait.instance then
            UIModalAddTrait.instance:close()
        end
        local modal = UIModalAddTrait:new()
        modal:initialise();
        modal:addToUIManager();
        modal:setAlwaysOnTop(true);
    end)
    self.addTrait:initialise();
    self.addTrait:instantiate();
    self.addTrait:setAnchorLeft(true);
    self.addTrait:setAnchorRight(false);
    self.addTrait:setAnchorTop(false);
    self.addTrait:setAnchorBottom(true);
    self.addTrait.isOnlyInGame = true;
    self.addTrait.isRequireSelected = false;
    self:addChild(self.addTrait);
    table.insert(self.buttonList, self.addTrait);

    self.deleteTrait = UIButton:new(self.addTrait.x + self.addTrait.width + 10, self.height - 80, 100, 24, getTranslate("UI_PlayerEditor_PlayerTraits_DeleteTrait"),
    function()
        if not self.localPlayer or not self.datas.selected or self.datas.selected < 1 then
            return
        end
        
        local selectedItem = self.datas.items[self.datas.selected]
        if not selectedItem or not selectedItem.item then
            return
        end
        
        -- PZ 42.x uses CharacterTraits
        if self.localPlayer.getCharacterTraits then
            local charTraits = self.localPlayer:getCharacterTraits()
            if charTraits then
                charTraits:remove(selectedItem.item)
                SyncXp(self.localPlayer)
                self:updateTraits()
            end
            return
        end
        
        -- PZ 41.x uses old getTraits()
        if self.localPlayer.getTraits and self.localPlayer:getTraits() then
            self.localPlayer:getTraits():remove(selectedItem.item:getType())
            SyncXp(self.localPlayer)
            self:updateTraits()
        end
    end)
    self.deleteTrait:initialise();
    self.deleteTrait:instantiate();
    self.deleteTrait:setAnchorLeft(true);
    self.deleteTrait:setAnchorRight(false);
    self.deleteTrait:setAnchorTop(false);
    self.deleteTrait:setAnchorBottom(true);
    self.deleteTrait.isOnlyInGame = true;
    self.deleteTrait.isRequireSelected = true;
    self:addChild(self.deleteTrait);
    table.insert(self.buttonList, self.deleteTrait);

    self:updateTraits();
end

--*********************************************************
--* Инициализация черт характера
--*********************************************************
function UITraitsTable:updateTraits()
    self.lastSelectedIndex = self.datas.selected or 0;
    self.datas:clear();

    if not self.localPlayer then
        return
    end
    
    -- PZ 42.x uses CharacterTraits system
    if self.localPlayer.getCharacterTraits then
        local charTraits = self.localPlayer:getCharacterTraits()
        if charTraits then
            local knownTraits = charTraits:getKnownTraits()
            if knownTraits then
                for i=0, knownTraits:size() - 1 do
                    local trait = knownTraits:get(i)
                    if trait ~= nil then
                        -- Store CharacterTrait object; drawDatas will look up definition
                        self.datas:addItem(trait:getName(), trait);
                    end
                end
            end
        end
        self.datas.selected = self.lastSelectedIndex;
        return
    end
    
    -- Fallback to old getTraits() method (PZ 41.x)
    if self.localPlayer.getTraits then
        local traits = self.localPlayer:getTraits()
        if traits then
            for i=0, traits:size() - 1 do
                local traitName = traits:get(i)
                local trait = TraitFactory.getTrait(traitName);
                if trait ~= nil then
                    if trait:getTexture() then
                        self.datas:addItem(trait:getLabel(), trait);
                    end
                end
            end
        end
    end
    
    self.datas.selected = self.lastSelectedIndex;
end

--*********************************************************
--* Обновление таблицы
--*********************************************************
function UITraitsTable:update()
    self.datas.doDrawItem = self.drawDatas;
    
    -- Check if traits are available (either old getTraits or new getCharacterTraits)
    local traitsAvailable = self.localPlayer and (self.localPlayer.getTraits or self.localPlayer.getCharacterTraits)
    
    for i=1, #self.buttonList do
        local item = self.buttonList[i];
        -- Disable trait buttons if no trait API available
        if not traitsAvailable then
            item:setEnable(false);
        elseif item.isOnlyInGame and (self.localPlayer == nil or self.localPlayer:isDead()) then
            item:setEnable(false);
        elseif (not self.datas.items[self.datas.selected] or #self.datas.items < 1) and item.isRequireSelected then
            item:setEnable(false);
        else
            item:setEnable(true);
        end
    end
end

--*********************************************************
--* Отрисовка данных
--*********************************************************
function UITraitsTable:drawDatas(y, item, alt)
    local clipY = math.max(0, y + self:getYScroll())
    local clipY2 = math.min(self.height, y + self:getYScroll() + self.itemheight)
    
    local scrollBarOffset = 14
    if self:getScrollHeight() < 70 then
        scrollBarOffset = 0
    end
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end

    self:suspendStencil()
    self:clampStencilRectToParent(0, clipY, self:getWidth() - scrollBarOffset, clipY2 - clipY)
    
    if self.selected == item.index then
        self:drawRect(0, y, self:getWidth(), self.itemheight, 0.3, EtherMain.accentColor.r, EtherMain.accentColor.g, EtherMain.accentColor.b);
    end

    if alt then
        self:drawRect(0, y, self:getWidth(), self.itemheight, 0.3, 0.3, 0.3, 0.3);
    end
    self:drawRectBorder(0, y, self:getWidth(), self.itemheight, 0.5, self.borderColor.r, self.borderColor.g, self.borderColor.b);
    self:drawRectBorder(self.columns[1].size, y, self.columns[2].size, self.itemheight, 0.5, self.borderColor.r, self.borderColor.g, self.borderColor.b);
    
    self:clearStencilRect()
    self:resumeStencil()
  
    local clipX = self.columns[1].size
    local clipX2 = self.columns[2].size
    local clipY = math.max(0, y + self:getYScroll())
    local clipY2 = math.min(self.height, y + self:getYScroll() + self.itemheight)

    -- Look up CharacterTraitDefinition for PZ 42.x traits
    local traitDef = nil
    local isPZ42Trait = false
    if item.item then
        -- Check if this is a PZ 42.x CharacterTrait (has getName but not getLabel)
        if item.item.getName and not item.item.getLabel then
            isPZ42Trait = true
            -- Try to get definition from CharacterTraitDefinition
            if CharacterTraitDefinition and CharacterTraitDefinition.getCharacterTraitDefinition then
                traitDef = CharacterTraitDefinition.getCharacterTraitDefinition(item.item)
            end
        elseif item.item.getLabel then
            -- PZ 41.x TraitFactory trait
            traitDef = item.item
        end
    end

    -- Draw item text
    self:suspendStencil()
    self:clampStencilRectToParent(clipX, clipY, clipX2 - clipX, clipY2 - clipY)
    local text = item.text or "Unknown"
    if traitDef and traitDef.getLabel then
        text = traitDef:getLabel()
    end
    self:drawText(text, 25, y + 4, 1, 1, 1, 1, UIFont.Small);
    self:clearStencilRect()
    self:resumeStencil()

    -- Draw description
    local description = ""
    if traitDef and traitDef.getDescription then
        description = traitDef:getDescription():gsub("\n", "; ")
    end
    
    self:suspendStencil()
    self:clampStencilRectToParent(self.columns[2].size, clipY, self.width - self.columns[2].size - scrollBarOffset, clipY2 - clipY)
    self:drawText(description, self.columns[2].size + 10, y + 4, 1, 1, 1, 1, UIFont.Small);
    self:clearStencilRect()
    self:resumeStencil()

    self:repaintStencilRect(0, clipY, self.width - scrollBarOffset, clipY2 - clipY)

    -- Draw texture from definition
    local iconX = 4
    local iconSize = fontHeightSmall;
    if traitDef and traitDef.getTexture then
        local texture = traitDef:getTexture()
        if texture then
            self:suspendStencil()
            self:clampStencilRectToParent(self.columns[1].size + iconX, clipY, iconSize, clipY2 - clipY)
            self:drawTextureScaledAspect2(texture, self.columns[1].size + iconX, y + (self.itemheight - iconSize) / 2, iconSize, iconSize,  1, 1, 1, 1);
            self:clearStencilRect()
            self:resumeStencil()
        end
    end
    return y + self.itemheight;
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function UITraitsTable:new (x, y, width, height)
    local menuTableData = ISPanel:new(x, y, width, height);
    setmetatable(menuTableData, self);
    menuTableData.borderColor = {r=0.4, g=0.4, b=0.4, a=0};
    menuTableData.backgroundColor = {r=0, g=0, b=0, a=0};
    menuTableData.localPlayer = getPlayer();
    menuTableData.lastSelectedIndex = 0;
    menuTableData.buttonList = {};
    menuTableData.updateTraits = self.updateTraits;
    UITraitsTable.instance = menuTableData;
    return menuTableData;
end