package dev.ninemmteam.mod.modules.impl.client;

import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import net.minecraft.util.Identifier;

public class CapeModule extends Module {
    public static CapeModule INSTANCE;

    public enum CapeType {
        TW_COUNTRY(Identifier.of("fentanyl", "textures/cape/twcountry.png"));
        private final Identifier texture;

        CapeType(Identifier texture) {
            this.texture = texture;
        }

        public Identifier getTexture() {
            return texture;
        }

        public String getDisplayName() {
            return "twcountry";
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
    }

    private final EnumSetting<CapeType> cape = this.add(new EnumSetting<>("Texture", CapeType.TW_COUNTRY));

    public CapeModule() {
        super("Cape", Category.Client);
        this.setChinese("斗篷");
        INSTANCE = this;
    }

    public CapeType getSelectedCape() {
        return cape.getValue();
    }

    public Identifier getSelectedTexture() {
        return getSelectedCape().getTexture();
    }

    public boolean isOn() {
        return super.isOn();
    }
}
