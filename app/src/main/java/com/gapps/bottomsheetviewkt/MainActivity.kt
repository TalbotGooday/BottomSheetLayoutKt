package com.gapps.bottomsheetviewkt

import android.animation.ArgbEvaluator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
	companion object {
		fun newInstance(context: Context) = Intent(context, MainActivity::class.java)
	}
	private val argbEvaluator = ArgbEvaluator()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		expand.setOnClickListener { layout.expand() }
		collapse.setOnClickListener { layout.collapse() }

		layout.setOnProgressListener {
			val bgColor = argbEvaluator.evaluate(it, -11247209,  -12095874) as Int

			content.setBackgroundColor(bgColor)

		}
	}
}
