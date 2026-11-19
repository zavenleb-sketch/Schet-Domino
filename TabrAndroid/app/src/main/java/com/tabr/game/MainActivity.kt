package com.tabr.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val gold = Color(0xFFD6B36A)
private val black = Color(0xFF101014)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TabrScreen() }
    }
}
@Composable fun TabrScreen() {
    var game by remember { mutableStateOf<TabrGame?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf(setOf<Int>()) }
    val g = game
    Column(Modifier.fillMaxSize().background(black).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("ТАБР", fontSize = 36.sp, color = gold)
        Text("BLACK & GOLD EDITION", color = gold)
        if(g == null) {
            Text("Локальный прототип · онлайн и боты в разработке", color = Color.White)
            Button(onClick = { game = TabrGame(2,31).also { it.startRound() } }) { Text("2 игрока · до 31") }
            Button(onClick = { game = TabrGame(4,51).also { it.startRound() } }) { Text("4 игрока · до 51") }
        } else {
            @Suppress("UNUSED_VARIABLE") val refresh = tick
            Text("Счёт: ${g.scores[0]} : ${g.scores[1]}  ·  Ход игрока ${g.turn+1}", color = Color.White)
            Text("Карты на столе", color = gold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(g.table) { i,c ->
                    val chosen = i in selected
                    Button(onClick = { selected = if(chosen) selected-i else selected+i },
                        colors = ButtonDefaults.buttonColors(containerColor = if(chosen) gold else Color(0xFF303039))) { Text(c.label) }
                }
            }
            Text("Карты игрока ${g.turn+1}", color = gold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(g.hands[g.turn]) { i,c ->
                    Button(onClick = {
                        val groups = if(selected.isEmpty()) emptyList() else listOf(selected.sorted())
                        if(groups.isEmpty() || g.legal(groups,c.rank)) {
                            g.play(i,groups); selected = emptySet(); tick++
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = black)) { Text(c.label) }
                }
            }
            Text("Выделите карты на столе, затем нажмите карту в руке. Без выделения карта выкладывается.", color = Color.LightGray)
            if(g.roundOver) {
                Text(if(g.matchOver) "Матч окончен" else "Раздача завершена", color = gold)
                if(!g.matchOver) Button(onClick = { g.startRound(); selected = emptySet(); tick++ }) { Text("Следующая раздача") }
            }
            Button(onClick = { game = null; selected = emptySet() }) { Text("В меню") }
        }
    }
}
