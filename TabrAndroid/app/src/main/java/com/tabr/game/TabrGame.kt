package com.tabr.game

import kotlin.random.Random

enum class Suit(val symbol: String) { CLUBS("♣"), DIAMONDS("♦"), HEARTS("♥"), SPADES("♠") }
data class Card(val suit: Suit, val rank: Int) {
    val label get() = (when(rank) { 1 -> "A"; 11 -> "J"; 12 -> "Q"; 13 -> "K"; else -> rank.toString() }) + suit.symbol
}
class TabrGame(val players: Int, val target: Int) {
    init { require(players == 2 || players == 4); require(target == 31 || target == 51) }
    val hands = List(players) { mutableListOf<Card>() }
    val piles = List(players) { mutableListOf<Card>() }
    val table = mutableListOf<Card>()
    val scores = IntArray(2)
    private val deck = mutableListOf<Card>()
    var starter = Random.nextInt(players); private set
    var turn = starter; private set
    var lastCaptor: Int? = null; private set
    var roundOver = false; private set
    var matchOver = false; private set
    var round = 0; private set
    data class Missed(val actor: Int, val rank: Int, val playedOnTable: Boolean, val playedCard: Card)
    private var awaitingTurnAdvance = false
    var missed: Missed? = null; private set
    fun team(player: Int) = if(players == 4) player % 2 else player
    fun startRound() {
        if(round > 0) starter = (starter + 1) % players
        round++
        turn = starter
        hands.forEach { it.clear() }; piles.forEach { it.clear() }; table.clear(); deck.clear()
        deck.addAll(Suit.entries.flatMap { s -> (1..13).map { Card(s,it) } }.shuffled())
        repeat(4) { table.add(deck.removeAt(0)) }
        deal()
        lastCaptor = null; roundOver = false; missed = null
    }
    private fun deal() {
        repeat(6) { for(p in 0 until players) if(deck.isNotEmpty()) hands[p].add(deck.removeAt(0)) }
    }
    fun legal(groups: List<List<Int>>, rank: Int): Boolean {
        val flat = groups.flatten()
        return groups.isNotEmpty() && flat.distinct().size == flat.size &&
            groups.all { it.isNotEmpty() && it.all { i -> i in table.indices } && it.sumOf { i -> table[i].rank } == rank }
    }
    private fun awardTabr(p: Int) {
        val cap = if(target == 31) 21 else 33
        if(scores[team(p)] < cap) scores[team(p)]++
    }
    private fun hasCombination(rank: Int, excluded: Int? = null): Boolean {
        val candidates = table.indices.filter { it != excluded && table[it].rank <= rank }
        fun find(start: Int, sum: Int): Boolean {
            if(sum == rank) return true
            if(sum > rank) return false
            for(i in start until candidates.size)
                if(find(i+1, sum + table[candidates[i]].rank)) return true
            return false
        }
        return find(0,0)
    }
    fun play(index: Int, groups: List<List<Int>> = emptyList()) {
        require(!roundOver && !matchOver && missed == null && index in hands[turn].indices)
        val actor = turn
        val rank = hands[actor][index].rank
        require(groups.isEmpty() || legal(groups,rank))
        val played = hands[actor].removeAt(index)
        val onTable = groups.isEmpty()
        if(onTable) table.add(played) else {
            piles[actor].add(played)
            groups.flatten().sortedDescending().forEach { piles[actor].add(table.removeAt(it)) }
            lastCaptor = actor
            val finalMove = deck.isEmpty() && hands.all { it.isEmpty() }
            if(table.isEmpty() && !finalMove) awardTabr(actor)
        }
        val playedIndex = if(onTable) table.lastIndex else null
        if(hasCombination(rank,playedIndex)) {
            missed = Missed(actor,rank,onTable,played)
            awaitingTurnAdvance = true
        } else advance()
    }
    private fun advance() {
        awaitingTurnAdvance = false
        if(hands.all { it.isEmpty() }) {
            if(deck.isEmpty()) {
                lastCaptor?.let { piles[it].addAll(table); table.clear() }
                scoreRound()
                roundOver = true
            } else { deal(); turn = starter }
        } else turn = (turn + 1) % players
    }
    fun passClaims() {
        require(missed != null && awaitingTurnAdvance)
        missed = null
        advance()
    }
    fun claimMissed(claimant: Int, groups: List<List<Int>>) {
        val m = requireNotNull(missed)
        require(awaitingTurnAdvance && claimant in 0 until players && claimant != m.actor)
        require(legal(groups,m.rank))
        val captured = groups.flatten().toSet()
        if(m.playedOnTable) {
            val playedIndex = table.indexOf(m.playedCard)
            require(playedIndex >= 0 && playedIndex !in captured)
        }
        val cards = captured.sortedDescending().map { table.removeAt(it) }
        piles[claimant].addAll(cards)
        if(m.playedOnTable) {
            val playedIndex = table.indexOf(m.playedCard)
            require(playedIndex >= 0)
            piles[claimant].add(table.removeAt(playedIndex))
        }
        lastCaptor = claimant
        val finalMove = deck.isEmpty() && hands.all { it.isEmpty() }
        if(table.isEmpty() && team(claimant) == team(m.actor) && !finalMove) awardTabr(claimant)
        // Allow further claims only if another independent combination remains.
        if(hasCombination(m.rank, null)) {
            missed = m.copy(playedOnTable = false)
        } else {
            missed = null
            advance()
        }
    }
    private fun scoreRound() {
        val cards = List(2) { t -> piles.indices.filter { team(it) == t }.flatMap { piles[it] } }
        for(t in 0..1) {
            scores[t] += cards[t].count { it.rank == 1 }
            if(cards[t].any { it.rank == 2 && it.suit == Suit.CLUBS }) scores[t]++
            if(cards[t].any { it.rank == 10 && it.suit == Suit.DIAMONDS }) scores[t] += 2
        }
        val clubs = cards.map { c -> c.count { it.suit == Suit.CLUBS } }
        if(clubs[0] != clubs[1]) scores[if(clubs[0] > clubs[1]) 0 else 1]++
        val counts = cards.map { it.size }
        if(counts[0] != counts[1]) scores[if(counts[0] > counts[1]) 0 else 1] += 2
        matchOver = scores.maxOrNull()!! >= target && scores[0] != scores[1]
    }
}
