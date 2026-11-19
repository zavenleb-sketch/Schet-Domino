package com.tabr.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val gold = Color(0xFFD9B86B)
private val ink = Color(0xFF101014)
private val felt = Color(0xFF10372D)
private val ivory = Color(0xFFFFF9EB)
private val red = Color(0xFFB51C30)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = 5894 or 1024 or 512
        setContent { MaterialTheme { TabrScreen() } }
    }
}

@Composable
private fun PlayingCard(card: Card, chosen: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val suitColor = if(card.suit == Suit.DIAMONDS || card.suit == Suit.HEARTS) red else ink
    Column(
        modifier = modifier.width(54.dp).height(75.dp)
            .border(if(chosen) 3.dp else 1.dp, if(chosen) gold else Color(0xFFC5BBA8), RoundedCornerShape(6.dp))
            .background(if(chosen) Color(0xFFFFE8A4) else ivory, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick).padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(card.label.dropLast(1), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = suitColor)
        Text(card.suit.symbol, fontSize = 23.sp, color = suitColor)
        Text(card.label.dropLast(1), fontSize = 11.sp, color = suitColor)
    }
}

@Composable
private fun BackCard(modifier: Modifier = Modifier) {
    Box(modifier.width(35.dp).height(49.dp).border(1.dp,gold,RoundedCornerShape(5.dp))
        .background(ink,RoundedCornerShape(5.dp)).padding(4.dp)
        .border(1.dp,gold.copy(alpha=.65f),RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center) {
        Text("✦", color = gold, fontSize = 16.sp)
    }
}

@Composable
private fun Fan(cards: List<Card>, selected: Set<Int>, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    BoxWithConstraints(modifier.height(82.dp), contentAlignment = Alignment.Center) {
        val cardWidth = 54.dp
        val step = if(cards.size <= 1) cardWidth else ((maxWidth - cardWidth) / (cards.size - 1)).coerceIn(19.dp,39.dp)
        val width = if(cards.isEmpty()) 0.dp else cardWidth + step * (cards.size - 1)
        Box(Modifier.width(width).height(82.dp)) {
            cards.forEachIndexed { index, card ->
                PlayingCard(card,index in selected,Modifier.offset(x = step * index, y = if(index in selected) 0.dp else 6.dp)) { onSelect(index) }
            }
        }
    }
}

@Composable
private fun OpponentSeat(player: Int, count: Int, active: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.background(if(active) gold.copy(alpha=.22f) else ink.copy(alpha=.55f),RoundedCornerShape(12.dp))
        .border(if(active) 2.dp else 1.dp,if(active) gold else Color.Gray,RoundedCornerShape(12.dp))
        .padding(horizontal = 8.dp, vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Игрок ${player+1}" + if(active) "  ● ХОД" else "",color = if(active) gold else ivory,fontSize = 11.sp)
        Box(Modifier.width(88.dp).height(52.dp),contentAlignment = Alignment.Center) {
            repeat(count.coerceAtMost(6)) { i -> BackCard(Modifier.offset(x = (i * 9 - (count.coerceAtMost(6)-1)*4).dp)) }
        }
    }
}

@Composable
fun TabrScreen() {
    var game by remember { mutableStateOf<TabrGame?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var mode by remember { mutableIntStateOf(2) }
    var target by remember { mutableIntStateOf(31) }
    var message by remember { mutableStateOf("Выберите карты стола, затем свою карту") }
    val g = game
    Column(Modifier.fillMaxSize().background(ink).padding(horizontal = 12.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ТАБР",fontSize = 25.sp,fontWeight = FontWeight.Black,color = gold)
            Text("BLACK & GOLD",fontSize = 10.sp,color = gold)
            if(g != null) {
                val currentRevision = revision
                Text("Счёт: ${g.scores[0]} : ${g.scores[1]}    ·    Раздача ${g.round}    ·    до ${g.target}   ·   #${currentRevision}",color = ivory,fontSize = 12.sp)
                TextButton(onClick = { game = null; selected = emptySet() }) { Text("Меню",color = gold) }
            }
        }
        if(g == null) {
            Spacer(Modifier.height(14.dp))
            Text("Выберите режим игры",color = ivory,fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(2 to "1 на 1",4 to "2 на 2").forEach { (n,label) ->
                    Button(onClick = { mode = n },colors = ButtonDefaults.buttonColors(containerColor = if(mode == n) gold else Color.DarkGray,contentColor = if(mode == n) ink else ivory)) { Text(label) }
                }
            }
            Text("До скольких очков?",color = ivory,fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(31,51).forEach { points ->
                    Button(onClick = { target = points },colors = ButtonDefaults.buttonColors(containerColor = if(target == points) gold else Color.DarkGray,contentColor = if(target == points) ink else ivory)) { Text(points.toString()) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Button(onClick = { game = TabrGame(mode,target).also { it.startRound() }; selected = emptySet(); revision++; message = "Выберите карты стола, затем свою карту" },colors = ButtonDefaults.buttonColors(containerColor = gold,contentColor = ink)) { Text("НАЧАТЬ ИГРУ") }
            Text("Локальная версия: игроки ходят по очереди на одном телефоне",color = Color.LightGray,fontSize = 11.sp)
        } else {
            val currentRevision = revision
            val tableCards = remember(g,currentRevision) { g.table.toList() }
            val handCards = remember(g,currentRevision) { g.hands[g.turn].toList() }
            val turn = g.turn
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).background(felt,RoundedCornerShape(28.dp))
                .border(2.dp,gold,RoundedCornerShape(28.dp))) {
                val w = maxWidth
                val h = maxHeight
                if(g.players == 2) {
                    if(turn != 1) OpponentSeat(1,g.hands[1].size,false,Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
                    else OpponentSeat(0,g.hands[0].size,false,Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
                } else {
                    val other = (turn + 2) % 4
                    OpponentSeat(other,g.hands[other].size,false,Modifier.align(Alignment.TopCenter).padding(top = 3.dp))
                    val left = (turn + 1) % 4
                    val right = (turn + 3) % 4
                    OpponentSeat(left,g.hands[left].size,false,Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
                    OpponentSeat(right,g.hands[right].size,false,Modifier.align(Alignment.CenterEnd).padding(end = 6.dp))
                }
                Column(Modifier.align(Alignment.Center).width(w * if(g.players == 4) .62f else .88f),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("КАРТЫ НА СТОЛЕ · ${tableCards.size}",color = gold,fontSize = 10.sp)
                    if(tableCards.isEmpty()) Text("Стол пуст",color = ivory)
                    Fan(tableCards,selected,Modifier.fillMaxWidth()) { i ->
                        if(!g.roundOver) selected = if(i in selected) selected-i else selected+i
                    }
                }
                Text("ИГРОК ${turn+1} · ВАШ ХОД",color = gold,fontSize = 11.sp,fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomCenter).background(ink,RoundedCornerShape(topStart = 10.dp,topEnd = 10.dp)).padding(horizontal = 14.dp,vertical = 3.dp))
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                Text("Игрок ${turn+1}",color = gold,fontSize = 12.sp)
                Fan(handCards,emptySet(),Modifier.weight(1f)) { i ->
                    if(g.roundOver || g.matchOver) return@Fan
                    val card = handCards.getOrNull(i) ?: return@Fan
                    val index = g.hands[g.turn].indexOf(card)
                    if(index < 0) { message = "Повторите выбор карты"; return@Fan }
                    val groups = if(selected.isEmpty()) emptyList() else listOf(selected.sorted())
                    if(groups.isNotEmpty() && !g.legal(groups,card.rank)) {
                        message = "Сумма выбранных карт должна быть ${card.rank}"
                        return@Fan
                    }
                    try {
                        g.play(index,groups)
                        selected = emptySet()
                        message = "Ход выполнен"
                        revision++
                    } catch(e: IllegalArgumentException) {
                        selected = emptySet()
                        message = "Неверный ход"
                        revision++
                    } catch(e: IllegalStateException) {
                        selected = emptySet()
                        message = "Повторите ход"
                        revision++
                    }
                }
                TextButton(onClick = { selected = emptySet() }) { Text("Сброс",color = gold,fontSize = 11.sp) }
                if(g.roundOver && !g.matchOver) Button(onClick = { g.startRound(); selected = emptySet(); revision++ },colors = ButtonDefaults.buttonColors(containerColor = gold,contentColor = ink)) { Text("Новая раздача",fontSize = 11.sp) }
                if(g.matchOver) Text("Матч окончен",color = gold)
            }
            Text(message,color = ivory,fontSize = 10.sp,maxLines = 1)
        }
    }
}
