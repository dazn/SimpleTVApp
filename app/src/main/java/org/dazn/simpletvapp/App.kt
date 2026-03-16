package org.dazn.simpletvapp

import android.app.Activity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient
import org.dazn.simpletvapp.data.repository.MediaRepository
import org.dazn.simpletvapp.presentation.browse.BrowseScreen
import org.dazn.simpletvapp.presentation.photo.PhotoScreen
import org.dazn.simpletvapp.presentation.photo.PhotoViewModelFactory
import org.dazn.simpletvapp.presentation.player.PlayerScreen
import java.net.URLEncoder

fun buildPlayerRoute(
    path: String,
    format: String,
    displayAspectRatio: String? = null,
    videoCodec: String? = null,
    audioCodec: String? = null,
): String {
    val encodedPath = URLEncoder.encode(path, "UTF-8").replace("+", "%20")
    val encodedFormat = URLEncoder.encode(format, "UTF-8").replace("+", "%20")
    var route = "player?path=$encodedPath&format=$encodedFormat"
    if (displayAspectRatio != null) route += "&displayAspectRatio=${URLEncoder.encode(displayAspectRatio, "UTF-8").replace("+", "%20")}"
    if (videoCodec != null) route += "&videoCodec=${URLEncoder.encode(videoCodec, "UTF-8").replace("+", "%20")}"
    if (audioCodec != null) route += "&audioCodec=${URLEncoder.encode(audioCodec, "UTF-8").replace("+", "%20")}"
    return route
}

fun buildPhotoRoute(path: String): String {
    val encoded = URLEncoder.encode(path, "UTF-8").replace("+", "%20")
    return "photo?path=$encoded"
}

@UnstableApi
@Composable
fun App() {
    val navController = rememberNavController()
    val activity = LocalContext.current as? Activity
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(
                    callFactory = {
                        OkHttpClient.Builder()
                            .addInterceptor { chain ->
                                chain.proceed(
                                    chain.request().newBuilder()
                                        .header("Authorization", "Bearer ${BuildConfig.API_KEY}")
                                        .build()
                                )
                            }
                            .build()
                    }
                ))
            }
            .build()
    }

    NavHost(
        navController = navController,
        startDestination = "browse",
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(
            route = "browse?path={path}",
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            BrowseScreen(
                onNavigateToPlayer = { path, format, displayAspectRatio, videoCodec, audioCodec ->
                    navController.navigate(buildPlayerRoute(path, format, displayAspectRatio, videoCodec, audioCodec))
                },
                onNavigateToPhoto = { path ->
                    navController.navigate(buildPhotoRoute(path))
                },
                onExitApp = { activity?.finish() }
            )
        }

        composable(
            route = "player?path={path}&format={format}&displayAspectRatio={displayAspectRatio}&videoCodec={videoCodec}&audioCodec={audioCodec}",
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" },
                navArgument("format") { type = NavType.StringType; defaultValue = "" },
                navArgument("displayAspectRatio") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("videoCodec") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("audioCodec") { type = NavType.StringType; nullable = true; defaultValue = null },
            )
        ) { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path") ?: ""
            PlayerScreen(
                path = path,
                displayAspectRatio = backStackEntry.arguments?.getString("displayAspectRatio"),
                videoCodec = backStackEntry.arguments?.getString("videoCodec"),
                audioCodec = backStackEntry.arguments?.getString("audioCodec"),
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "photo?path={path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path") ?: ""
            val vm: org.dazn.simpletvapp.presentation.photo.PhotoViewModel = viewModel(
                factory = PhotoViewModelFactory(MediaRepository(), path)
            )
            PhotoScreen(
                viewModel = vm,
                imageLoader = imageLoader,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
