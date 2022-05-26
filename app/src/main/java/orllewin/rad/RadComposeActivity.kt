package orllewin.rad

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import orllewin.rad.ui.theme.RADTheme
import orllewin.rad.views.RadIndeterminate
import orllewin.rad.views.RemoteLogo

@AndroidEntryPoint
class RadComposeActivity : ComponentActivity() {

    private val viewModel: RadViewModel by viewModels()


    private val urlPrefKey = stringPreferencesKey("radio_feed_url")
    private var feedUrl = "https://orllewin.uk/orllewin_stations.json"
    private lateinit var prefs: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = runBlocking { applicationContext.dataStore.data.first() }
        feedUrl = prefs[urlPrefKey] ?: "https://orllewin.uk/orllewin_stations.json"

        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "stations") {
                composable("stations") { Stations(navController) }
                composable("settings") { Settings(navController) }
            }
        }

        viewModel.getRadStations(feedUrl)
    }

    @Composable
    fun Stations(navController: NavController) {
        RADTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                RadScreen(viewModel, navController)
            }
        }
    }

    private fun playStation(radPlayable: StationEntity) {
        println("RAD: playstation: ${radPlayable.title}")
        startService(RadService.getIntent(this, radPlayable))
    }

    private fun stop(){
        println("RAD: stop()")
        startService(RadService.getStopIntent(this))
    }

    @OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
    @Composable
    fun RadScreen(viewModel: RadViewModel, navController: NavController) {
        val coroutineScope = rememberCoroutineScope()
        var showMenu by remember { mutableStateOf(false) }
        var selectedStation by remember { mutableStateOf<StationEntity?>(null) }
        var bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
        )

        val uriHandler = LocalUriHandler.current

        when {
            viewModel.uiState.loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    RadIndeterminate().Indeterminate(80.dp, 16.dp)
                    Image(
                        painter = painterResource(R.drawable.vector_app_icon),
                        contentDescription = "Radio",
                        modifier = Modifier.size(150.dp)
                    )
                }
            }
            else -> {
                BottomSheetScaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Image(
                                    painter = painterResource(id = R.drawable.vector_radio),
                                    contentDescription = "Radio",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            },
                            elevation = 0.dp,
                            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                            actions = {
                                IconButton(onClick = { showMenu = !showMenu }) {
                                    Icon(Icons.Default.MoreVert, "")
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(onClick = {
                                        showMenu = false
                                        navController.navigate("settings")
                                    }) {
                                        Text("Settings")
                                    }
                                }
                            }
                        )
                    },
                    content = {
                            LazyVerticalGrid(
                                cells = GridCells.Fixed(3),
                                state = rememberLazyListState(),
                                contentPadding = PaddingValues(10.dp)
                            ) {
                                items(viewModel.uiState.stations.size) { index ->
                                    val station = viewModel.uiState.stations[index]
                                    Card(
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clickable {
                                                selectedStation = station
                                                playStation(selectedStation!!)
                                                coroutineScope.launch {
                                                    bottomSheetScaffoldState.bottomSheetState.expand()
                                                }
                                            },
                                        backgroundColor = MaterialTheme.colors.surface,
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            if (station.hasLogoUrl()) {
                                                RemoteLogo().RemoteLogoFill(
                                                    station.logoUrl!!,
                                                    station.title
                                                )
                                            } else {
                                                Text(
                                                    text = station.title,
                                                    fontSize = 20.sp,
                                                    modifier = Modifier.padding(start = 0.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                    },
                    scaffoldState = bottomSheetScaffoldState,
                    //Bottom Sheet
                    sheetContent = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            selectedStation?.let {
                                Row {
                                    RemoteLogo().RemoteLogo(
                                        selectedStation!!.logoUrl,
                                        selectedStation!!.title
                                    ){
                                        uriHandler.openUri(selectedStation!!.website!!)
                                    }
                                    Column(
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                                    ) {
                                        Text(text = "${selectedStation?.title}", fontSize = 20.sp)
                                        Button(
                                            onClick = {
                                                stop()
                                                coroutineScope.launch{
                                                    bottomSheetScaffoldState.bottomSheetState.collapse()
                                                }

                                            }
                                        ) {
                                            Icon(
                                                painterResource(R.drawable.vector_stop),
                                                contentDescription = "Stop",
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                            Text("Stop")
                                        }
                                    }
                                }
                            }
                        }
                    }, sheetPeekHeight = 0.dp
                )
            }
        }
    }

    //--------------------------------------------------------------------------------------SETTINGS
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Settings(navController: NavController) {
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current

        var url by rememberSaveable { mutableStateOf(feedUrl) }
        val keyboardController = LocalSoftwareKeyboardController.current

        RADTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Settings")
                            },
                            elevation = 0.dp,
                            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                            navigationIcon = {
                                IconButton(onClick = { navController.navigateUp() }) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        )
                    },
                    content =
                    {
                        Column {
                            //Update remote card
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                    .fillMaxWidth(),
                                backgroundColor = MaterialTheme.colors.surface,
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Set JSON feed Url",
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                    TextField(
                                        value = url,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .fillMaxWidth(),
                                        colors = defaultTextInputStyle(),
                                        onValueChange = { nu -> url = nu },
                                        label = { Text("Feed url (https://)") }
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                when {
                                                    url.isBlank() -> toast("Enter station feed Url")
                                                    !url.startsWith("https://") -> toast("Stations feed Url must start with https://")
                                                    else ->{
                                                        coroutineScope.launch {
                                                            context.dataStore.edit { mPrefs ->
                                                                mPrefs[urlPrefKey] = url
                                                            }

                                                            viewModel.getRadStations(url)
                                                            navController.navigateUp()
                                                        }
                                                    }
                                                }
                                            }
                                        ){
                                            Icon(
                                                Icons.Filled.Done,
                                                contentDescription = "Save",
                                                modifier = Modifier.size(ButtonDefaults.IconSize)
                                            )
                                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                            Text("Save")
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }


    @Composable
    private fun defaultTextInputStyle(): TextFieldColors {
        return TextFieldDefaults.textFieldColors(
            textColor = Color.Black,
            focusedLabelColor = Color.DarkGray,
            backgroundColor = Color.White,
            cursorColor = Color.Black
        )
    }

    fun toast(message: String) = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
}