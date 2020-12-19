/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.dessertpusher

import android.content.ActivityNotFoundException
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleObserver
import com.example.android.dessertpusher.databinding.ActivityMainBinding
import timber.log.Timber
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MainActivity : AppCompatActivity(), LifecycleObserver {

    private var revenue = 0
    private var dessertsSold = 0

    // Contains all the views
    private lateinit var binding: ActivityMainBinding

    /** Dessert Data **/

    /**
     * Simple data class that represents a dessert. Includes the resource id integer associated with
     * the image, the price it's sold for, and the startProductionAmount, which determines when
     * the dessert starts to be produced.
     */
    data class Dessert(val imageId: Int, val price: Int, val startProductionAmount: Int)

    // region Lifecycle Logging
    private lateinit var createTime: LocalDateTime

    private lateinit var pauseTime: LocalDateTime

    private lateinit var restartTime: LocalDateTime

    private lateinit var lowMemoryTime: LocalDateTime

    private lateinit var stopTime: LocalDateTime

    private var restarted: Boolean = false

    private var paused: Boolean = false

    private var showingDialog: Boolean = false
    // endregion

    // Create a list of all desserts, in order of when they start being produced
    private val allDesserts = listOf(
            Dessert(R.drawable.cupcake, 5, 0),
            Dessert(R.drawable.donut, 10, 5),
            Dessert(R.drawable.eclair, 15, 20),
            Dessert(R.drawable.froyo, 30, 50),
            Dessert(R.drawable.gingerbread, 50, 100),
            Dessert(R.drawable.honeycomb, 100, 200),
            Dessert(R.drawable.icecreamsandwich, 500, 500),
            Dessert(R.drawable.jellybean, 1000, 1000),
            Dessert(R.drawable.kitkat, 2000, 2000),
            Dessert(R.drawable.lollipop, 3000, 4000),
            Dessert(R.drawable.marshmallow, 4000, 8000),
            Dessert(R.drawable.nougat, 5000, 16000),
            Dessert(R.drawable.oreo, 6000, 20000)
    )
    private var currentDessert = allDesserts[0]

    private lateinit var dessertTimer: DessertTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        // region Lifecycle Logging
        createTime = LocalDateTime.now()
        restarted = false
        Timber.i("onCreate() called at $createTime")
        // endregion
        super.onCreate(savedInstanceState)

        // Use Data Binding to get reference to the views
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.dessertButton.setOnClickListener {
            onDessertClicked()
        }

        // Set the TextViews to the right values
        binding.revenue = revenue
        binding.amountSold = dessertsSold

        // Make sure the correct dessert is showing
        binding.dessertButton.setImageResource(currentDessert.imageId)

        dessertTimer = DessertTimer()
    }

    /**
     * Updates the score when the dessert is clicked. Possibly shows a new dessert.
     */
    private fun onDessertClicked() {

        // Update the score
        revenue += currentDessert.price
        dessertsSold++

        binding.revenue = revenue
        binding.amountSold = dessertsSold

        // Show the next dessert
        showCurrentDessert()
    }

    /**
     * Determine which dessert to show.
     */
    private fun showCurrentDessert() {
        var newDessert = allDesserts[0]
        for (dessert in allDesserts) {
            if (dessertsSold >= dessert.startProductionAmount) {
                newDessert = dessert
            }
            // The list of desserts is sorted by startProductionAmount. As you sell more desserts,
            // you'll start producing more expensive desserts as determined by startProductionAmount
            // We know to break as soon as we see a dessert who's "startProductionAmount" is greater
            // than the amount sold.
            else break
        }

        // If the new dessert is actually different than the current dessert, update the image
        if (newDessert != currentDessert) {
            currentDessert = newDessert
            binding.dessertButton.setImageResource(newDessert.imageId)
        }
    }

    /**
     * Menu methods
     */
    private fun onShare() {
        val shareIntent = ShareCompat.IntentBuilder.from(this)
                .setText(getString(R.string.share_text, dessertsSold, revenue))
                .setType("text/plain")
                .intent
        try {
            startActivity(shareIntent)
            showingDialog = true
        } catch (ex: ActivityNotFoundException) {
            showingDialog = false
            Toast.makeText(this, getString(R.string.sharing_not_available),
                    Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.shareMenuButton -> onShare()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRestart() {
        // region Lifecycle Logging
        restartTime = LocalDateTime.now()
        restarted = true
        Timber.i("onRestart() called at $restartTime")
        // endregion
        super.onRestart()
        // TODO Put anything that runs only if not being created
    }

    override fun onStart() {
        // region Lifecycle Logging
        if (restarted) {
            Timber.i("onStart() called after onRestart() after ${millisBetweenRestartTimeAndNow()}")
        } else {
            Timber.i("onStart() called after ${millisBetweenCreateTimeAndNow()}")
        }
        // endregion
        super.onStart()
        // TODO Initialize/Start objects that only run when Activity on screen
        // TODO Permanently save data
        dessertTimer.startTimer()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // region Lifecycle Logging
        Timber.i("onConfigurationChanged() called")
        // endregion
        super.onConfigurationChanged(newConfig)
    }

    override fun onResume() {
        // region Lifecycle Logging
        when {
            paused && showingDialog -> {
                paused = false
                showingDialog = false
                Timber.i("onResume() called after onPause()")
            }
            restarted ->
                Timber.i("onResume() called after onRestart() after ${millisBetweenRestartTimeAndNow()}")
            else -> Timber.i("onResume() called after ${millisBetweenCreateTimeAndNow()}")
        }
        // endregion
        super.onResume()
    }

    override fun onPause() {
        // region Lifecycle Logging
        paused = true
        pauseTime = LocalDateTime.now()
        Timber.i("onPause() called at $pauseTime")
        // endregion
        super.onPause()
        // TODO Blocks UI from drawing; keep this light-weight
    }

    override fun onStop() {
        // region Lifecycle Logging
        stopTime = LocalDateTime.now()
        Timber.i("onStop() called after ${millisBetweenPauseTimeAndNow()}")
        // endregion
        super.onStop()
        // TODO Uninitialize/Stop objects that only run when Activity on screen
        dessertTimer.stopTimer()
    }

    override fun onLowMemory() {
        // region Lifecycle Logging
        lowMemoryTime = LocalDateTime.now()
        Timber.i("onLowMemory() called at $lowMemoryTime}")
        // endregion
        super.onLowMemory()
    }

    override fun onDestroy() {
        // region Lifecycle Logging
        @Suppress("SENSELESS_COMPARISON")
        when {
            lowMemoryTime != null ->
                Timber.i("onDestroy() called after onLowMemory() after ${millisBetweenLowMemoryTimeAndNow()}")
            else ->
                Timber.i("onDestroy() called after onStop() after ${millisBetweenStopTimeAndNow()}")
        }
        // endregion
        super.onDestroy()
    }

    // region Lifecycle Logging
    private fun millisBetweenCreateTimeAndNow() =
            "${ChronoUnit.MILLIS.between(createTime, LocalDateTime.now())}ms"

    private fun millisBetweenRestartTimeAndNow() =
            "${ChronoUnit.MILLIS.between(restartTime, LocalDateTime.now())}ms"

    private fun millisBetweenPauseTimeAndNow() =
            "${ChronoUnit.MILLIS.between(pauseTime, LocalDateTime.now())}ms"

    private fun millisBetweenLowMemoryTimeAndNow() =
            "${ChronoUnit.MILLIS.between(lowMemoryTime, LocalDateTime.now())}ms"

    private fun millisBetweenStopTimeAndNow() =
            "${ChronoUnit.MILLIS.between(stopTime, LocalDateTime.now())}ms"
    // endregion
}
