package com.vurka.memoji.data

import com.vurka.memoji.model.EmojiEntry
import kotlin.random.Random

object EmojiCatalog {

    private fun group(category: String, vararg raw: String): List<EmojiEntry> =
        raw.map { item ->
            val emoji = item.substringBefore(' ')
            EmojiEntry(id = emoji, emoji = emoji, category = category, name = item.substringAfter(' '))
        }

    val items: List<EmojiEntry> = (
        group("Animals",
            "🐶 Dog", "🐱 Cat", "🦊 Fox", "🐼 Panda", "🐨 Koala", "🦁 Lion", "🐯 Tiger", "🐰 Rabbit",
            "🐸 Frog", "🦉 Owl", "🦄 Unicorn", "🐧 Penguin", "🐝 Bee", "🦋 Butterfly", "🐙 Octopus",
            "🐳 Whale", "🐵 Monkey", "🐮 Cow", "🐷 Pig", "🐻 Bear", "🐔 Chicken", "🐢 Turtle",
            "🐬 Dolphin", "🦈 Shark", "🐞 Ladybug", "🦀 Crab", "🐊 Crocodile", "🦒 Giraffe",
            "🦓 Zebra", "🐘 Elephant", "🦔 Hedgehog", "🐹 Hamster") +
        group("Wild",
            "🐛 Caterpillar", "🐜 Ant", "🦗 Cricket", "🕷️ Spider", "🦂 Scorpion", "🐌 Snail", "🦆 Duck",
            "🦅 Eagle", "🦜 Parrot", "🦢 Swan", "🦩 Flamingo", "🐦 Bird", "🦇 Bat", "🐺 Wolf", "🐗 Boar",
            "🐴 Horse", "🐑 Sheep", "🐐 Goat", "🦌 Deer", "🦏 Rhino", "🐫 Camel", "🦘 Kangaroo") +
        group("Food",
            "🍕 Pizza", "🍔 Burger", "🍟 Fries", "🍣 Sushi", "🍩 Donut", "🍪 Cookie", "🍦 Ice Cream",
            "🍓 Strawberry", "🍉 Watermelon", "🥑 Avocado", "🍿 Popcorn", "🧁 Cupcake", "🍎 Apple",
            "🍌 Banana", "🍇 Grapes", "🍒 Cherries", "🍑 Peach", "🍍 Pineapple", "🥕 Carrot", "🌽 Corn",
            "🥐 Croissant", "🧀 Cheese", "🌮 Taco", "🍜 Noodles", "🍰 Cake", "🍫 Chocolate", "🍭 Lollipop",
            "🥞 Pancakes") +
        group("Nature",
            "✨ Sparkles", "⭐ Star", "🌈 Rainbow", "☀️ Sun", "🌙 Moon", "🔥 Fire", "🌸 Blossom",
            "🌵 Cactus", "🌴 Palm", "❄️ Snowflake", "🌻 Sunflower", "🌹 Rose", "🍀 Clover", "🍁 Maple Leaf",
            "🌲 Pine", "🌊 Wave", "⚡ Lightning", "☁️ Cloud", "🍄 Mushroom", "🌺 Hibiscus", "💧 Droplet",
            "🌷 Tulip") +
        group("Faces",
            "😀 Grin", "😍 Heart Eyes", "😎 Cool", "🥳 Party Face", "🤔 Thinking", "😇 Angel", "👻 Ghost",
            "👽 Alien", "🤖 Robot", "🤡 Clown", "💀 Skull", "😉 Wink", "😂 Joy", "🥰 Love", "😴 Sleepy",
            "🤩 Star-Struck", "😱 Scream", "🤠 Cowboy", "🤓 Nerd", "😜 Silly", "🥶 Cold", "🥵 Hot",
            "😈 Devil", "🤗 Hug") +
        group("Sports",
            "⚽ Soccer", "🏀 Basketball", "🏆 Trophy", "🥇 Gold Medal", "🎮 Video Game", "🎸 Guitar",
            "🎨 Palette", "🚀 Rocket", "🏈 Football", "🎾 Tennis", "🎯 Bullseye", "🎲 Dice", "⚾ Baseball",
            "🏐 Volleyball", "🏉 Rugby", "🎱 Pool", "🏓 Ping Pong", "🥊 Boxing", "⛳ Golf", "🎳 Bowling",
            "🏹 Archery", "🛹 Skateboard", "🎻 Violin", "🥁 Drum") +
        group("Symbols",
            "💎 Gem", "👑 Crown", "🔑 Key", "🔒 Lock", "🎁 Gift", "🎈 Balloon", "🔔 Bell", "💡 Bulb",
            "🔋 Battery", "🧭 Compass", "⏳ Hourglass", "⚓ Anchor", "💰 Money Bag", "🔮 Crystal Ball",
            "🧲 Magnet", "🔭 Telescope", "🔬 Microscope", "📷 Camera", "⌚ Watch", "📱 Phone") +
        group("Space",
            "🌌 Galaxy", "🌠 Shooting Star", "🛸 UFO", "🛰️ Satellite", "🌑 New Moon", "🌓 Half Moon",
            "🌕 Full Moon", "🌟 Glowing Star", "💫 Dizzy Star", "☄️ Comet", "🌞 Sun Face", "🌝 Moon Face",
            "🌛 Moon Left", "🌜 Moon Right", "🌚 Dark Moon", "🌎 Earth", "🌏 Asia Earth", "🌐 Globe",
            "🌘 Waning Moon", "🌗 Last Quarter") +
        group("Travel",
            "🚗 Car", "🚕 Taxi", "🚌 Bus", "🚓 Police Car", "🚑 Ambulance", "🚒 Fire Truck", "🚜 Tractor",
            "🚲 Bicycle", "🛵 Scooter", "🚂 Train", "✈️ Plane", "🚁 Helicopter", "⛵ Sailboat", "🚢 Ship",
            "🚤 Speedboat", "🏰 Castle", "🗼 Tower", "🗽 Statue", "🎡 Ferris Wheel", "🎢 Roller Coaster",
            "⛺ Tent", "🏖️ Beach") +
        group("Fantasy",
            "🐉 Dragon", "🐲 Dragon Face", "🧙 Wizard", "🧚 Fairy", "🧛 Vampire", "🧜 Merperson", "🧝 Elf",
            "🧞 Genie", "🧟 Zombie", "🦖 T-Rex", "🦕 Dino", "🔱 Trident", "⚔️ Swords", "🛡️ Shield",
            "🗡️ Dagger", "🧿 Evil Eye", "🎭 Masks", "📜 Scroll", "🏺 Vase", "🧪 Potion", "👹 Ogre", "👺 Goblin") +
        group("Party",
            "🎧 Headphones", "🎤 Mic", "🎹 Piano", "🎷 Sax", "🎼 Score", "🎬 Clapper", "🎠 Carousel",
            "🎉 Party Popper", "🎊 Confetti", "🎂 Birthday Cake", "🥂 Cheers", "🎀 Ribbon", "🧸 Teddy",
            "🎄 Tree", "🎃 Pumpkin", "🎆 Fireworks", "🎇 Sparkler", "🧨 Firecracker")
        ).distinctBy { it.id }

    fun getByCategory(category: String): List<EmojiEntry> = items.filter { it.category == category }

    fun getAllCategories(): List<String> = items.map { it.category }.distinct()

    /**
     * Builds a shuffled pool of at least [requiredCount] distinct emojis that
     * preferentially come from [categories]. If the chosen categories cannot
     * supply enough unique emojis, the rest are filled from the full catalog so
     * deck generation never fails.
     */
    fun getPoolForCategories(
        categories: List<String>,
        requiredCount: Int,
        random: Random
    ): List<EmojiEntry> {
        if (requiredCount <= 0) return emptyList()

        val preferred = if (categories.isEmpty()) emptyList() else items.filter { it.category in categories }
        val pool = preferred.shuffled(random).toMutableList()
        if (pool.size < requiredCount) {
            val preferredIds = preferred.mapTo(HashSet()) { it.id }
            pool.addAll(items.filter { it.id !in preferredIds }.shuffled(random))
        }
        return pool
    }
}
