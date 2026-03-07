package dev.ninemmteam.mod.gui.earth.frame;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.mod.gui.earth.EarthClickGui;
import dev.ninemmteam.mod.gui.earth.component.impl.ModuleComponent;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.ClickGui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CategoryFrame extends ModulesFrame {
    private final Module.Category category;

    public CategoryFrame(Module.Category category, float posX, float posY, float width, float height) {
        super(category.toString(), posX, posY, width, height);
        this.category = category;
        this.setExtended(true);
    }

    @Override
    public void init() {
        getComponents().clear();
        float offsetY = getHeight() + 1;
        
        if (fentanyl.MODULE == null) {
            return;
        }
        
        List<Module> moduleList = fentanyl.MODULE.getModulesByCategory(category);
        if (moduleList == null) {
            return;
        }

        try {
            if (ClickGui.getInstance() != null && ClickGui.getInstance().abc.getValue()) {
                moduleList.sort(Comparator.comparing(Module::getName));
            }
        } catch (Exception e) {
        }
        
        String searchText = null;
        try {
            if (EarthClickGui.getInstance() != null) {
                searchText = EarthClickGui.getInstance().getSearchText();
            }
        } catch (Exception e) {
        }
        
        List<Module> filteredList = new ArrayList<>();
        
        if (searchText != null && !searchText.isEmpty()) {
            for (Module module : moduleList) {
                if (module.getName().toLowerCase().contains(searchText.toLowerCase())) {
                    filteredList.add(module);
                }
            }
        } else {
            filteredList = moduleList;
        }
        
        for (Module module : filteredList) {
            getComponents().add(new ModuleComponent(module, getPosX(), getPosY(), 0, offsetY, getWidth(), 12));
            offsetY += 12;
        }
        super.init();
    }
    
    public void resort() {
        if (fentanyl.MODULE == null || getComponents().isEmpty()) {
            return;
        }
        
        try {
            List<Module> moduleList = fentanyl.MODULE.getModulesByCategory(category);
            if (moduleList == null) {
                return;
            }

            if (ClickGui.getInstance() != null && ClickGui.getInstance().abc.getValue()) {
                moduleList.sort(Comparator.comparing(Module::getName));
            }
            
            String searchText = null;
            try {
                if (EarthClickGui.getInstance() != null) {
                    searchText = EarthClickGui.getInstance().getSearchText();
                }
            } catch (Exception e) {
            }
            
            List<Module> filteredList = new ArrayList<>();
            
            if (searchText != null && !searchText.isEmpty()) {
                for (Module module : moduleList) {
                    if (module.getName().toLowerCase().contains(searchText.toLowerCase())) {
                        filteredList.add(module);
                    }
                }
            } else {
                filteredList = moduleList;
            }
            
            java.util.Map<String, Boolean> expandedStates = new java.util.HashMap<>();
            for (var component : new ArrayList<>(getComponents())) {
                if (component instanceof ModuleComponent) {
                    ModuleComponent mc = (ModuleComponent) component;
                    expandedStates.put(mc.getModule().getName(), mc.isExtended());
                }
            }
            
            getComponents().clear();
            float offsetY = getHeight() + 1;
            
            for (Module module : filteredList) {
                ModuleComponent mc = new ModuleComponent(module, getPosX(), getPosY(), 0, offsetY, getWidth(), 12);
                if (expandedStates.containsKey(module.getName())) {
                    mc.setExtended(expandedStates.get(module.getName()));
                }
                getComponents().add(mc);
                offsetY += 12;
            }
            
            for (var component : new ArrayList<>(getComponents())) {
                if (component instanceof ModuleComponent) {
                    ((ModuleComponent) component).init();
                }
            }
        } catch (Exception e) {
        }
    }

    public Module.Category getCategory() {
        return category;
    }
}
