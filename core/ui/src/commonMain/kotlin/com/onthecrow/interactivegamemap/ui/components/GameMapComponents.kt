package com.onthecrow.interactivegamemap.ui.components

import androidx.compose.runtime.Immutable
import org.koin.core.scope.Scope

/** The components of a game's style; the shared screens draw with them through the Game* composables. */
@Immutable
class GameMapComponents(
    val button: ButtonComponent,
    val textButton: TextButtonComponent,
    val iconButton: IconButtonComponent,
    val mapButton: MapButtonComponent,
    val zoomControl: ZoomControlComponent,
    val searchField: SearchFieldComponent,
    val switch: SwitchComponent,
    val chip: ChipComponent,
    val menu: MenuComponent,
    val menuItem: MenuItemComponent,
    val panel: PanelComponent,
    val divider: DividerComponent,
    val sheet: SheetComponent,
    val choiceDialog: ChoiceDialogComponent,
    val groupHeader: GroupHeaderComponent,
    val categoryItem: CategoryItemComponent,
    val visibilityActions: VisibilityActionsComponent,
    val cycleCategory: CycleCategoryComponent,
    val cycleTab: CycleTabComponent,
    val screenScaffold: ScreenScaffoldComponent,
    val sectionHeader: SectionHeaderComponent,
    val settingsGroup: SettingsGroupComponent,
    val settingsRow: SettingsRowComponent,
    val progressIndicator: ProgressIndicatorComponent,
)

/** The components a game registered in Koin one by one, together. */
fun Scope.gameMapComponents(): GameMapComponents = GameMapComponents(
    button = get(),
    textButton = get(),
    iconButton = get(),
    mapButton = get(),
    zoomControl = get(),
    searchField = get(),
    switch = get(),
    chip = get(),
    menu = get(),
    menuItem = get(),
    panel = get(),
    divider = get(),
    sheet = get(),
    choiceDialog = get(),
    groupHeader = get(),
    categoryItem = get(),
    visibilityActions = get(),
    cycleCategory = get(),
    cycleTab = get(),
    screenScaffold = get(),
    sectionHeader = get(),
    settingsGroup = get(),
    settingsRow = get(),
    progressIndicator = get(),
)
