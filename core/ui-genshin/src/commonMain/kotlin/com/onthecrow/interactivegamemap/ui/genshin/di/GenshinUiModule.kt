package com.onthecrow.interactivegamemap.ui.genshin.di

import com.onthecrow.interactivegamemap.ui.components.ButtonComponent
import com.onthecrow.interactivegamemap.ui.components.CategoryItemComponent
import com.onthecrow.interactivegamemap.ui.components.ChipComponent
import com.onthecrow.interactivegamemap.ui.components.ChoiceDialogComponent
import com.onthecrow.interactivegamemap.ui.components.CycleCategoryComponent
import com.onthecrow.interactivegamemap.ui.components.CycleTabComponent
import com.onthecrow.interactivegamemap.ui.components.DividerComponent
import com.onthecrow.interactivegamemap.ui.components.GroupHeaderComponent
import com.onthecrow.interactivegamemap.ui.components.IconButtonComponent
import com.onthecrow.interactivegamemap.ui.components.MapButtonComponent
import com.onthecrow.interactivegamemap.ui.components.MenuComponent
import com.onthecrow.interactivegamemap.ui.components.MenuItemComponent
import com.onthecrow.interactivegamemap.ui.components.PanelComponent
import com.onthecrow.interactivegamemap.ui.components.ProgressIndicatorComponent
import com.onthecrow.interactivegamemap.ui.components.ScreenScaffoldComponent
import com.onthecrow.interactivegamemap.ui.components.SearchFieldComponent
import com.onthecrow.interactivegamemap.ui.components.SectionHeaderComponent
import com.onthecrow.interactivegamemap.ui.components.SettingsGroupComponent
import com.onthecrow.interactivegamemap.ui.components.SettingsRowComponent
import com.onthecrow.interactivegamemap.ui.components.SheetComponent
import com.onthecrow.interactivegamemap.ui.components.SwitchComponent
import com.onthecrow.interactivegamemap.ui.components.TextButtonComponent
import com.onthecrow.interactivegamemap.ui.components.VisibilityActionsComponent
import com.onthecrow.interactivegamemap.ui.components.ZoomControlComponent
import com.onthecrow.interactivegamemap.ui.components.gameMapComponents
import com.onthecrow.interactivegamemap.ui.genshin.GenshinButton
import com.onthecrow.interactivegamemap.ui.genshin.GenshinCategoryItem
import com.onthecrow.interactivegamemap.ui.genshin.GenshinChip
import com.onthecrow.interactivegamemap.ui.genshin.GenshinChoiceDialog
import com.onthecrow.interactivegamemap.ui.genshin.GenshinCycleCategory
import com.onthecrow.interactivegamemap.ui.genshin.GenshinCycleTab
import com.onthecrow.interactivegamemap.ui.genshin.GenshinDivider
import com.onthecrow.interactivegamemap.ui.genshin.GenshinGroupHeader
import com.onthecrow.interactivegamemap.ui.genshin.GenshinIconButton
import com.onthecrow.interactivegamemap.ui.genshin.GenshinMapButton
import com.onthecrow.interactivegamemap.ui.genshin.GenshinMenu
import com.onthecrow.interactivegamemap.ui.genshin.GenshinMenuItem
import com.onthecrow.interactivegamemap.ui.genshin.GenshinPanel
import com.onthecrow.interactivegamemap.ui.genshin.GenshinScreenScaffold
import com.onthecrow.interactivegamemap.ui.genshin.GenshinSearchField
import com.onthecrow.interactivegamemap.ui.genshin.GenshinSectionHeader
import com.onthecrow.interactivegamemap.ui.genshin.GenshinSettingsGroup
import com.onthecrow.interactivegamemap.ui.genshin.GenshinSettingsRow
import com.onthecrow.interactivegamemap.ui.genshin.GenshinSheet
import com.onthecrow.interactivegamemap.ui.genshin.GenshinStyle
import com.onthecrow.interactivegamemap.ui.genshin.GenshinProgressIndicator
import com.onthecrow.interactivegamemap.ui.genshin.GenshinSwitch
import com.onthecrow.interactivegamemap.ui.genshin.GenshinTextButton
import com.onthecrow.interactivegamemap.ui.genshin.GenshinVisibilityActions
import com.onthecrow.interactivegamemap.ui.genshin.GenshinZoomControl
import com.onthecrow.interactivegamemap.ui.theme.GameMapStyle
import org.koin.dsl.module

/** The Genshin Impact look for GameMapTheme. */
val genshinUiModule = module {
    single<ButtonComponent> { GenshinButton }
    single<TextButtonComponent> { GenshinTextButton }
    single<IconButtonComponent> { GenshinIconButton }
    single<MapButtonComponent> { GenshinMapButton }
    single<ZoomControlComponent> { GenshinZoomControl }
    single<SearchFieldComponent> { GenshinSearchField }
    single<SwitchComponent> { GenshinSwitch }
    single<ChipComponent> { GenshinChip }
    single<MenuComponent> { GenshinMenu }
    single<MenuItemComponent> { GenshinMenuItem }
    single<PanelComponent> { GenshinPanel }
    single<DividerComponent> { GenshinDivider }
    single<SheetComponent> { GenshinSheet }
    single<ChoiceDialogComponent> { GenshinChoiceDialog }
    single<GroupHeaderComponent> { GenshinGroupHeader }
    single<CategoryItemComponent> { GenshinCategoryItem }
    single<VisibilityActionsComponent> { GenshinVisibilityActions }
    single<CycleCategoryComponent> { GenshinCycleCategory }
    single<CycleTabComponent> { GenshinCycleTab }
    single<ScreenScaffoldComponent> { GenshinScreenScaffold }
    single<SectionHeaderComponent> { GenshinSectionHeader }
    single<SettingsGroupComponent> { GenshinSettingsGroup }
    single<SettingsRowComponent> { GenshinSettingsRow }
    single<ProgressIndicatorComponent> { GenshinProgressIndicator }
    single { gameMapComponents() }
    single<GameMapStyle> { GenshinStyle(get()) }
}
