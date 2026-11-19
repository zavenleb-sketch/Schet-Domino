package com.tabr.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val gold = Color(0xFFD9B86B)
private val ink = Color(0xFF101014)
private val felt = Color(0xFF10372D)
private val ivory = Color(0xFFFFF9EB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { TabrScreen() } }
    }
}

@Composable
private fun PlayingCard(card: Card, chosen: Boolean, onClick: () -> Unit) {
    val red = card.suit == Suit.DIAMONDS || card.suit == Suit.HEARTS
    Column(
        modifier = Modifier.width(65.dp).height(99.dp)
            .border(BorderStroke(if(chosen) 3.dp else 1.dp, if(chosen) gold else Color(0xFFC5BBA8)), RoundedCornerShape(8.dp))
            .background(if(chosen) Color(0xFFFFE8A4) else ivory, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(card.label.dropLast(1), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if(red) Color(0xFFB51C30) else ink)
        Text(card.suit.symbol, fontSize = 27.sp, color = if(red) Color(0xFFB51C30) else ink)
        Text(card.label.dropLast(1), fontSize = 14.sp, color = if(red) Color(0xFFB51C30) else ink)
    }
}

@Composable
fun TabrScreen() {
    var game by remember { mutableStateOf<TabrGame?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var message by remember { mutableStateOf("Выберите карту на столе, затем карту в руке") }
    val g = game
    Column(Modifier.fillMaxSize().background(ink).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ТАБР", fontSize = 38.sp, fontWeight = FontWeight.Black, color = gold)
        Text("BLACK & GOLD EDITION", fontSize = 12.sp, letterSpacing = 2.sp, color = gold)
        if(g == null) {
            Spacer(Modifier.height(36.dp))
            Text("Локальный тестовый режим", color = ivory)
            Button(onClick = { game = TabrGame(2,31).also { it.startRound() }; selected = emptySet() }, colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = ink)) { Text("2 игрока · до 31") }
            Button(onClick = { game = TabrGame(4,51).also { it.startRound() }; selected = emptySet() }, colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = ink)) { Text("4 игрока · до 51") }
            Text("Игра на одном телефоне. Боты и онлайн пока не подключены.", color = Color.LightGray, textAlign = TextAlign.Center)
        } else {
            // Reading revision explicitly ensures recomposition after mutations of the engine's mutable lists.
            val currentRevision = revision
            val active = g.turn
            val tableSnapshot = remember(g,currentRevision) { g.table.toList() }
            val handSnapshot = remember(g,currentRevision) { g.hands[g.turn].toList() }
            Text("СЧЁТ   ${g.scores[0]}  :  ${g.scores[1]}     ·     РАЗДАЧА ${g.round}", color = gold, fontWeight = FontWeight.Bold)
            Text("Ход игрока ${active+1}", color = ivory, fontSize = 18.sp)
            Column(Modifier.fillMaxWidth().weight(1f).background(felt, RoundedCornerShape(24.dp))
                .border(2.dp,gold,RoundedCornerShape(24.dp)).padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ИГРОВОЙ СТОЛ", color = gold, fontSize = 12.sp, letterSpacing = 2.sp)
                if(tableSnapshot.isEmpty()) Text("Стол пуст", color = ivory)
                LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    itemsIndexed(tableSnapshot) { index, card ->
                        PlayingCard(card, index in selected) {
                            if(!g.roundOver) selected = if(index in selected) selected-index else selected+index
                        }
                    }
                }
                Text("На столе: ${tableSnapshot.size}", color = Color.LightGray)
            }
            Text("ВАШИ КАРТЫ", color = gold, fontWeight = FontWeight.Bold)
            LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                itemsIndexed(handSnapshot) { index, card ->
                    PlayingCard(card,false) {
                        // Resolve by card identity in the CURRENT hand, not stale LazyRow index.
                        if(g.roundOver || g.matchOver) return@PlayingCard
                        val currentIndex = g.hands[g.turn].indexOf(card)
                        if(currentIndex < 0) { message = "Карты обновились. Повторите ход."; return@PlayingCard }
                        val groups = if(selected.isEmpty()) emptyList() else listOf(selected.sorted())
                        if(groups.isNotEmpty() && !g.legal(groups,card.rank)) {
                            message = "Сумма выбранных карт должна равняться ${card.rank}"
                            return@PlayingCard
                        }
                        try {
                            g.play(currentIndex,groups)
                            selected = emptySet()
                            message = "Ход выполнен"
                            revision++
                        } catch(e: IllegalArgumentException) {
                            selected = emptySet()
                            message = "Некорректный ход. Попробуйте ещё раз."
                            revision++
                        } catch(e: IllegalStateException) {
                            selected = emptySet()
                            message = "Ход устарел. Попробуйте ещё раз."
                            revision++
                        }
                    }
                }
            }
            Text(message, color = ivory, fontSize = 12.sp, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { selected = emptySet() }, border = BorderStroke(1.dp,gold)) { Text("Сброс выбора", color = gold) }
                if(g.roundOver && !g.matchOver) Button(onClick = { g.startRound(); selected = emptySet(); revision++ }, colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = ink)) { Text("Новая раздача") }
                if(g.matchOver) Text("Матч окончен", color = gold)
            }
            TextButton(onClick = { game = null; selected = emptySet() }) { Text("В меню", color = gold) }
        }
    }
}
