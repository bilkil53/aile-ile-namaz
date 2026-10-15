package com.example.data.util

import java.util.Calendar

data class DailyQuote(
    val category: String,
    val title: String,
    val text: String,
    val arabic: String? = null,
    val source: String,
    val isDiyanet: Boolean = false
)

data class SpiritualGreeting(
    val id: String,
    val title: String,
    val category: String, // "Tebrik", "Dua", "İstikrar", "Kaza"
    val iconEmoji: String,
    val text: String
)

object DailyContentProvider {

    val STATIC_GREETINGS_AND_DUAS = listOf(
        SpiritualGreeting(
            id = "greet_1",
            title = "Vakit Namazı Tebriki",
            category = "Tebrik",
            iconEmoji = "🌟",
            text = "Allah kabul etsin, kıldığın namazlar kalbine nur, ömrüne huzur ve bereket olsun kardeşim."
        ),
        SpiritualGreeting(
            id = "greet_2",
            title = "Samimi Kabul Duası",
            category = "Dua",
            iconEmoji = "🤲",
            text = "Rabbim kıldığın namazları, ettiğin duaları dergâh-ı izzetinde en güzel şekilde makbul eylesin."
        ),
        SpiritualGreeting(
            id = "greet_3",
            title = "İstikrar ve Gayret Takdiri",
            category = "İstikrar",
            iconEmoji = "🌿",
            text = "Namaz dinin direğidir. Birlikte hayırda yarışarak istikrarımızı koruduğumuz için Elhamdülillah!"
        ),
        SpiritualGreeting(
            id = "greet_4",
            title = "Hayırlı ve Huzurlu Gün",
            category = "Dua",
            iconEmoji = "🕊️",
            text = "Rabbim sana ve ailene afiyet, kalbine inşirah, hanene bereket ihsan eylesin."
        ),
        SpiritualGreeting(
            id = "greet_5",
            title = "Kaza Namazı Teşviki",
            category = "Kaza",
            iconEmoji = "📿",
            text = "Kaza namazlarını eritmeye devam; Rabbim azmini, gayretini ve sevabını kat kat artırsın."
        ),
        SpiritualGreeting(
            id = "greet_6",
            title = "Günün Tamamlanma Şükrü",
            category = "Tebrik",
            iconEmoji = "🌙",
            text = "Günün farzlarını tamamlayarak huzurla geceye kavuşturan Yüce Allah'a hamdolsun."
        )
    )

    private val AYETLER = listOf(
        DailyQuote(
            category = "Günün Ayeti · Diyanet Meali",
            title = "Namazın Önemi",
            arabic = "إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَوْقُوتًا",
            text = "Şüphesiz namaz, mü’minler üzerine belirli vakitlerde farz kılınmıştır.",
            source = "Nisâ Sûresi, 103. Âyet (Diyanet İşleri Başkanlığı Meali)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Günün Ayeti · Diyanet Meali",
            title = "Huşû ve Kurtuluş",
            arabic = "قَدْ أَفْلَحَ الْمُؤْمِنُونَ ﴿١﴾ الَّذِينَ هُمْ فِي صَلَاتِهِمْ خَاشِعُونَ",
            text = "Müminler gerçekten kurtuluşa ermişlerdir; onlar ki, namazlarında derin bir saygı ve huşû içindedirler.",
            source = "Mü'minûn Sûresi, 1-2. Âyetler (Diyanet İşleri Başkanlığı Meali)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Günün Ayeti · Diyanet Meali",
            title = "Sabır ve Namazla Yardım",
            arabic = "وَاسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ",
            text = "Sabır ve namaz ile Allah'tan yardım isteyin. Şüphesiz o, huşû duyanlardan başkasına pek ağır gelir.",
            source = "Bakara Sûresi, 45. Âyet (Diyanet İşleri Başkanlığı Meali)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Günün Ayeti · Diyanet Meali",
            title = "Kötülükten Alıkoyan Namaz",
            arabic = "إِنَّ الصَّلَاةَ تَنْهَىٰ عَنِ الْفَحْشَاءِ وَالْمُنْكَرِ",
            text = "Şüphesiz namaz, insanı hayasızlıktan ve her türlü kötülükten alıkoyar.",
            source = "Ankebût Sûresi, 45. Âyet (Diyanet İşleri Başkanlığı Meali)",
            isDiyanet = true
        )
    )

    private val HADISLER = listOf(
        DailyQuote(
            category = "Günün Hadis-i Şerifi · Diyanet",
            title = "Namaz Dinin Direğidir",
            arabic = "الصَّلَاةُ عِمَادُ الدِّينِ",
            text = "Namaz dinin direğidir. Kim onu dosdoğru kılarsa dinini ikame etmiş olur; kim de onu terk ederse dinini yıkmış olur.",
            source = "Beyhakî, Şu'abü'l-Îmân (Diyanet Hadislerle İslam)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Günün Hadis-i Şerifi · Diyanet",
            title = "Cemaatle Namazın Fazileti",
            text = "Cemaatle kılınan namaz, tek başına kılınan namazdan yirmi yedi derece daha faziletlidir.",
            source = "Buhârî, Ezân, 30; Müslim, Mesâcid, 249",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Günün Hadis-i Şerifi · Diyanet",
            title = "Kulun Rabbine En Yakın Olduğu An",
            arabic = "أَقْرَبُ مَا يَكُونُ الْعَبْدُ مِنْ رَبِّهِ وَهُوَ سَاجِدٌ",
            text = "Kulun Rabbine en yakın olduğu an, secde anıdır. Öyleyse secdede çokça dua ediniz.",
            source = "Müslim, Salât, 215 (Diyanet İşleri Başkanlığı)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Günün Hadis-i Şerifi · Diyanet",
            title = "Günahları Arındıran Beş Vakit Namaz",
            text = "Birinizin kapısı önünden bir nehir aksa ve o kimse nehirde her gün beş kere yıkansa üzerinde hiç kir kalır mı? İşte beş vakit namaz da böyledir; Allah onunla günahları siler.",
            source = "Buhârî, Mevâkît, 6; Müslim, Mesâcid, 283",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Günün Hadis-i Şerifi · Diyanet",
            title = "Kıyamette İlk Hesaba Çekilecek Amel",
            text = "Kıyamet gününde kulun ilk hesaba çekileceği amel namazdır. Eğer namazı düzgün çıkarsa kurtulur ve kazanır.",
            source = "Tirmizî, Salât, 188 (Diyanet Hadislerle İslam)",
            isDiyanet = true
        )
    )

    private val DUALAR = listOf(
        DailyQuote(
            category = "Günün Duası · Manevi Niyaz",
            title = "İbadette Süreklilik Duası",
            arabic = "رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِنْ ذُرِّيَّتِي ۚ رَبَّنَا وَتَقَبَّلْ دُعَاءِ",
            text = "Ey Rabbim! Beni ve neslimi namazı dosdoğru ve devamlı kılanlardan eyle. Ey Rabbimiz, dualarımızı kabul buyur!",
            source = "İbrâhim Sûresi, 40. Âyet Meali / Manevi Dua",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Günün Duası · Manevi Niyaz",
            title = "Gönül Huzuru ve Doğruluk",
            arabic = "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ",
            text = "Ey kalpleri halden hale çeviren Allah'ım! Kalbimizi rızana, ibadetine ve dinine sabit kıl. Bizi namazla huzur bulanlardan eyle.",
            source = "Tirmizî, De'avât, 120 (Manevi Niyaz)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Günün Duası · Manevi Niyaz",
            title = "Tövbe ve Kaza Azmi",
            text = "Allah'ım! Geçmişte kaçırdığımız, kusurlu kıldığımız namazlarımızı bağışla. Kaza borçlarımızı şevkle ve ihlasla tamamlamayı bizlere kolaylaştır.",
            source = "Manevi Dua ve İltica",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Günün Duası · Manevi Niyaz",
            title = "Rıza ve Cennet Duası",
            text = "Rabbena âtina fi'd-dünya haseneten ve fi'l-âhireti haseneten ve kınâ azâbe'n-nâr. Rabbimiz! Bize dünyada da iyilik ver, ahirette de iyilik ver ve bizi cehennem azabından koru.",
            source = "Bakara Sûresi, 201. Âyet",
            isDiyanet = true
        )
    )

    private val MOTIVASYONLAR = listOf(
        DailyQuote(
            category = "Bugünün Manevi Hatırlatması",
            title = "Namaz Vakti Bir Fırsattır",
            text = "Her ezan, dünyanın telaşından sıyrılıp Yaratıcının huzuruna çıkmak için sana sunulan eşsiz bir randevudur. Bugün vakitlerini erteleme.",
            source = "Aile ile Namaz Manevi Hatırlatma",
            isDiyanet = false
        ),
        DailyQuote(
            category = "Bugünün Manevi Hatırlatması",
            title = "Birlikte Daha Güçlüyüz",
            text = "Aile fertlerinin 'Namazını kıldın mı?' teşviki, cennet yolunda birbirine uzatılan en samimi eldir. Aileni teşvik etmeyi unutma!",
            source = "Aile ile Namaz Manevi Hatırlatma",
            isDiyanet = false
        ),
        DailyQuote(
            category = "Bugünün Manevi Hatırlatması",
            title = "Kaza Borcu Dağ Değildir",
            text = "Kılınan her bir kaza namazı, ruhun üzerindeki bir yükü hafifletir. Küçük adımların istikrarı, büyük engelleri aşmanın en kutlu yoludur.",
            source = "Aile ile Namaz Manevi Hatırlatma",
            isDiyanet = false
        )
    )

    private val OZLU_SOZLER = listOf(
        DailyQuote(
            category = "Günün Özlü Sözü · Hikmet",
            title = "Secdenin Kıymeti",
            text = "Secde, yere fısıldayıp göklerin ötesinden işitildiğin en yakın ve en samimi makamdır.",
            source = "Gönül Dünyasından Hikmet Damlaları",
            isDiyanet = false
        ),
        DailyQuote(
            category = "Günün Özlü Sözü · Hikmet",
            title = "Vaktin Bereketi",
            text = "Zamanın efendisi olmak istiyorsan, vaktini namazla tanzim et; çünkü namaz ömrün nizamıdır.",
            source = "İslami Tefekkür ve Hikmet",
            isDiyanet = false
        )
    )

    fun getDailyQuotes(calendar: Calendar = Calendar.getInstance()): List<DailyQuote> {
        val day = calendar.get(Calendar.DAY_OF_YEAR)
        val ayet = AYETLER[day % AYETLER.size]
        val hadis = HADISLER[day % HADISLER.size]
        val dua = DUALAR[day % DUALAR.size]
        val motivasyon = MOTIVASYONLAR[day % MOTIVASYONLAR.size]
        return listOf(ayet, hadis, dua, motivasyon)
    }

    fun getRandomRotatingNotificationQuote(customDay: Int? = null): DailyQuote {
        val cal = Calendar.getInstance()
        val day = customDay ?: cal.get(Calendar.DAY_OF_YEAR)
        // Her gün günün ayeti, hadisi yada duası sırasıyla saat 12'de iletilir
        val cycle = day % 3
        return when (cycle) {
            0 -> {
                val q = AYETLER[day % AYETLER.size]
                q.copy(category = "Günün Ayeti · Diyanet Meali")
            }
            1 -> {
                val q = HADISLER[day % HADISLER.size]
                q.copy(category = "Günün Hadis-i Şerifi · Diyanet")
            }
            else -> {
                val q = DUALAR[day % DUALAR.size]
                q.copy(category = "Günün Duası · Manevi Niyaz")
            }
        }
    }

    fun getPrayerApproachingQuote(prayerName: String, customDay: Int? = null): DailyQuote {
        val cleanName = prayerName.lowercase()
        return when {
            cleanName.contains("sabah") || cleanName.contains("imsak") -> {
                DailyQuote(
                    category = "Diyanet · Hadis-i Şerif & Sabah Niyazı",
                    title = "Sabah Namazının Fazileti",
                    text = "Kim sabah namazını kılarsa, o kimse Allah'ın güvencesi ve himayesi altındadır.",
                    source = "Müslim, Mesâcid, 262 (Diyanet Hadislerle İslam)",
                    isDiyanet = true
                )
            }
            cleanName.contains("öğle") || cleanName.contains("ogle") -> {
                DailyQuote(
                    category = "Diyanet · Hadis-i Şerif & Öğle Vakti",
                    title = "Gök Kapılarının Açıldığı An",
                    text = "Öğle vakti, sema kapılarının açıldığı ve salih amellerin Allah Teâlâ'ya yükseldiği mübarek bir vakittir.",
                    source = "Tirmizî, Vitir, 15 (Diyanet İşleri Başkanlığı)",
                    isDiyanet = true
                )
            }
            cleanName.contains("ikindi") -> {
                DailyQuote(
                    category = "Diyanet Meali · Âyet-i Kerime & İkindi",
                    title = "Orta Namaza Dikkat Edin",
                    text = "Namazlara ve orta namaza (ikindi namazına) devam edin; gönülden boyun eğerek Allah'ın huzurunda durun.",
                    source = "Bakara Sûresi, 238. Âyet (Diyanet İşleri Başkanlığı Meali)",
                    isDiyanet = true
                )
            }
            cleanName.contains("akşam") || cleanName.contains("aksam") -> {
                DailyQuote(
                    category = "Diyanet · Hadis-i Şerif & Akşam Niyazı",
                    title = "Dua ve İltica Vakti",
                    text = "Akşam vakti girdiğinde dualarınıza sarılın; günün yorgunluğunu huzur ve şükür secdesiyle taçlandırın.",
                    source = "Ebû Dâvûd, Salât, 9 (Diyanet İşleri Başkanlığı)",
                    isDiyanet = true
                )
            }
            cleanName.contains("yatsı") || cleanName.contains("yatsi") -> {
                DailyQuote(
                    category = "Diyanet · Hadis-i Şerif & Yatsı ve Gece",
                    title = "Yatsı Namazının Müjdesi",
                    text = "Yatsı namazını cemaatle kılan kimse, gecenin yarısını ibadet ve namazla ihya etmiş gibi ecir kazanır.",
                    source = "Müslim, Mesâcid, 260 (Diyanet İşleri Başkanlığı Hadislerle İslam)",
                    isDiyanet = true
                )
            }
            else -> {
                getRandomRotatingNotificationQuote(customDay)
            }
        }
    }
}
