package com.onthecrow.interactivegamemap.ui.rdr2.di

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
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2Button
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2CategoryItem
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2Chip
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2ChoiceDialog
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2CycleCategory
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2CycleTab
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2Divider
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2GroupHeader
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2IconButton
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2MapButton
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2Menu
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2MenuItem
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2Panel
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2ScreenScaffold
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2SearchField
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2SectionHeader
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2SettingsGroup
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2SettingsRow
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2Sheet
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2Style
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2Switch
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2TextButton
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2VisibilityActions
import com.onthecrow.interactivegamemap.ui.rdr2.Rdr2ZoomControl
import com.onthecrow.interactivegamemap.ui.theme.GameMapStyle
import org.koin.dsl.module

/** The Red Dead Redemption 2 look for GameMapTheme: Material components in the Material colors. */
val rdr2UiModule = module {
    single<ButtonComponent> { Rdr2Button }
    single<TextButtonComponent> { Rdr2TextButton }
    single<IconButtonComponent> { Rdr2IconButton }
    single<MapButtonComponent> { Rdr2MapButton }
    single<ZoomControlComponent> { Rdr2ZoomControl }
    single<SearchFieldComponent> { Rdr2SearchField }
    single<SwitchComponent> { Rdr2Switch }
    single<ChipComponent> { Rdr2Chip }
    single<MenuComponent> { Rdr2Menu }
    single<MenuItemComponent> { Rdr2MenuItem }
    single<PanelComponent> { Rdr2Panel }
    single<DividerComponent> { Rdr2Divider }
    single<SheetComponent> { Rdr2Sheet }
    single<ChoiceDialogComponent> { Rdr2ChoiceDialog }
    single<GroupHeaderComponent> { Rdr2GroupHeader }
    single<CategoryItemComponent> { Rdr2CategoryItem }
    single<VisibilityActionsComponent> { Rdr2VisibilityActions }
    single<CycleCategoryComponent> { Rdr2CycleCategory }
    single<CycleTabComponent> { Rdr2CycleTab }
    single<ScreenScaffoldComponent> { Rdr2ScreenScaffold }
    single<SectionHeaderComponent> { Rdr2SectionHeader }
    single<SettingsGroupComponent> { Rdr2SettingsGroup }
    single<SettingsRowComponent> { Rdr2SettingsRow }
    single { gameMapComponents() }
    single<GameMapStyle> { Rdr2Style(get()) }
}
