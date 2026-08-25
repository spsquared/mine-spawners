package me.katanya04.minespawners.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * The mod configuration screen, accesible from the "Mods" button in the main menu.
 * The configuration applies to clientside/single player... When using the mod serverside
 * only, just modify the value on the config toml file.
 */
@Environment(EnvType.CLIENT)
public class ConfigScreen extends OptionsSubScreen {
    protected final OptionInstance<Double> slider;
    private List<Item> pickaxes;
    protected PickaxesList pickaxesList;
    private final boolean inWorld;

    protected ConfigScreen(Screen previousScreen) {
        super(previousScreen, null, Component.translatable("config.title"));
        this.inWorld = Minecraft.getInstance().level != null;
        this.slider = new OptionInstance<>(
                "config.drop_chance",
                OptionInstance.noTooltip(),
                ConfigScreen::percentValueLabel,
                OptionInstance.UnitDouble.INSTANCE,
                (double) SimpleConfig.DROP_CHANCE.getValue(),
                SimpleConfig.DROP_CHANCE::setValue
        );
        // Defer pickaxe list initialization to avoid accessing DataComponents before they're bound
    }

    protected List<Item> getPickaxes() {
        if (!inWorld) {
            // Return empty list if not in world - components aren't bound yet
            return Collections.emptyList();
        }
        if (this.pickaxes == null) {
            try {
                // Try to sort by harvest level if components are available
                this.pickaxes = SimpleConfig.getAllPickaxes().stream().sorted(
                        (p1, p2) -> weirdRounding(getHarvestLevel(p1) - getHarvestLevel(p2))
                ).toList();
            } catch (Exception e) {
                // Components not yet bound - return empty list
                this.pickaxes = Collections.emptyList();
            }
        }
        return this.pickaxes;
    }

    protected int weirdRounding(double x) {
        return (int) (x > 0 ? Math.ceil(x) : Math.floor(x));
    }

    protected double getHarvestLevel(Item pickaxe) {
        return (pickaxe.getDefaultInstance().get(DataComponents.TOOL) == null ?
                1 : pickaxe.getDefaultInstance().get(DataComponents.TOOL).rules().stream()
                .filter(r -> r.speed().isPresent()).mapToDouble(r -> r.speed().get()).max().orElse(1))
                * (pickaxe.getDefaultInstance().get(DataComponents.MAX_DAMAGE) == null ?
                1 : pickaxe.getDefaultInstance().get(DataComponents.MAX_DAMAGE));
    }

    private static Component percentValueLabel(Component p_231898_, double p_231899_) {
        return Component.translatable("options.percent_value", p_231898_, (int)(p_231899_ * 100.0));
    }

    @Override
    protected void addOptions() {
        this.list.addBig(slider);
    }

    @Override
    protected void addContents() {
        this.list = this.layout.addToContents(new OptionsList(this.minecraft, this.width, this) {
            @Override
            public void updateSize(int width, @NotNull HeaderAndFooterLayout layout) {
                this.updateSizeAndPosition(width, ConfigScreen.this.slider.createButton(null).getHeight() + 10, layout.getHeaderHeight());
            }
        });
        this.list.setHeight(slider.createButton(null).getHeight() + 10);

        if (inWorld) {
            this.pickaxesList = this.layout.addToContents(new PickaxesList(this, this.minecraft));
        } else {
            // Show message that user needs to join a world to configure pickaxe blacklist
            MultiLineTextWidget messageWidget = new MultiLineTextWidget(
                    Component.translatable("config.join_world_message"),
                    this.font
            );
            messageWidget.setCentered(true);
            this.layout.addToContents(messageWidget);
        }

        this.addOptions();
    }

    @Override
    protected void repositionElements() {
        super.repositionElements();
        if (inWorld && this.pickaxesList != null) {
            this.pickaxesList.updateSize(this.width, this.layout);
        }
    }

    @Override
    public void onClose() {
        SimpleConfig.saveToFile();
        super.onClose();
    }
}
