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
        val day = customDay ?: (cal.get(Calendar.DAY_OF_YEAR) + cal.get(Calendar.YEAR))
        // Her gün saat 12:00'de: Ya dua, ya hadis, ya da özlü bir söz (tek ve 1 kereye mahsus)
        val allOzluSozler = OZLU_SOZLER + MOTIVASYONLAR
        val choice = (day + (cal.get(Calendar.DAY_OF_MONTH))) % 3
        return when (choice) {
            0 -> {
                val q = DUALAR[day % DUALAR.size]
                q.copy(category = "Günün Duası 🤲")
            }
            1 -> {
                val q = HADISLER[day % HADISLER.size]
                q.copy(category = "Günün Hadis-i Şerifi 📜")
            }
            else -> {
                val q = allOzluSozler[day % allOzluSozler.size]
                q.copy(category = "Günün Özlü Sözü 💡")
            }
        }
    }

    private val SABAH_APPROACHING_QUOTES = listOf(
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & Sabah Niyazı",
            title = "Sabah Namazının Fazileti",
            text = "Kim sabah namazını kılarsa, o kimse Allah'ın güvencesi ve himayesi altındadır.",
            source = "Müslim, Mesâcid, 262 (Diyanet Hadislerle İslam)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & Sabah Bereketi",
            title = "Dünyadan Hayırlı İki Rekât",
            text = "Sabah namazının iki rekât sünneti, dünyadan ve dünyadaki her şeyden daha hayırlıdır.",
            source = "Müslim, Müsâfirîn, 96 (Diyanet İşleri Başkanlığı)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet Meali · Âyet-i Kerime & Sabah",
            title = "Şahitli Sabah Namazı",
            arabic = "إِنَّ قُرْآنَ الْفَجْرِ كَانَ مَشْهُودًا",
            text = "Gündüzün güneş dönüp batmaya meylettiği andan gecenin karanlığına kadar namaz kıl; bir de sabah namazını. Çünkü sabah namazı meleklerin şahit olduğu bir namazdır.",
            source = "İsrâ Sûresi, 78. Âyet (Diyanet Meali)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Sabah Duası ve İltica",
            title = "Sabahın Nuru ve Afiyeti",
            text = "Allah'ım! Sabahın bereketini, hayrını, nurunu ve hidayetini Sen'den niyaz ederiz. Bizi ve ailemizi sabah namazının nurlu iklimiyle bereketlendir.",
            source = "Ebû Dâvûd, Edeb, 101 & Diyanet Niyaz",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & Meleklerin Şahitliği",
            title = "Meleklerin Buluşma Vakti",
            text = "Gece ve gündüz melekleri sabah namazında toplanırlar. Sabah namazı ibadetin ve günün en bereketli anahtarıdır.",
            source = "Buhârî, Mevâkît, 16 (Diyanet Hadislerle İslam)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Manevi Niyaz & Seher Vakti",
            title = "Seherde Huzura Durmak",
            text = "Ey Rabbim! Seher vaktinde uyanıp huzuruna yöneldik. Bizi gaflet uykusundan uyandır, kalbimizi namazla aydınlat, dualarımızı kabul eyle.",
            source = "Diyanet Manevi Dua ve İltica",
            isDiyanet = true
        )
    )

    private val CUMA_APPROACHING_QUOTES = listOf(
        DailyQuote(
            category = "Diyanet · Cuma Sûresi & Cuma Namazı Vakti 🕌",
            title = "Hayırlı Cumalar · Cuma Namazı Vakti",
            text = "Ey iman edenler! Cuma günü namaza çağrıldığınızda Allah'ı anmaya koşun ve alışverişi bırakın. Eğer bilirseniz bu sizin için daha hayırlıdır.",
            source = "Cuma Sûresi, 9. Âyet (Diyanet İşleri Başkanlığı)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & Cuma Müjdesi",
            title = "Haftanın En Hayırlı Günü",
            text = "Üzerine güneşin doğduğu en hayırlı gün Cuma günüdür. O günde öyle mübarek bir icabet saati vardır ki, Müslüman kul o anda dua ederse Allah duasını kabul buyurur.",
            source = "Müslim, Cum'a, 18 (Diyanet Hadislerle İslam)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & Cuma Namazı Fazileti",
            title = "Cuma Namazı ve Bağışlanma",
            text = "Cuma namazına güzelce hazırlanıp huşû ile camiye giden kimsenin iki cuma arasındaki kusurları bağışlanır.",
            source = "Buhârî, Cum'a, 6; Müslim, Cum'a, 27",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & Peygamberimize Salavat",
            title = "Cuma Salavatı ve Selam",
            text = "Cuma günü bana çokça salavat getiriniz; çünkü sizin salavatlarınız bana arz olunur. Allahümme salli alâ seyyidinâ Muhammedin ve alâ âli seyyidinâ Muhammed.",
            source = "Ebû Dâvûd, Salât, 201 (Diyanet İşleri Başkanlığı)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Cuma Niyazı ve Rahmet",
            title = "Cuma Bereketi ve Aile Duası",
            text = "Rabbimiz! Bu mübarek Cuma gününde hürmetine ailemizi koru, dualarımızı ve tevbelerimizi makbul eyle, hanelerimize huzur ve bereket ihsan buyur.",
            source = "Diyanet Cuma Duası",
            isDiyanet = true
        )
    )

    private val OGLE_APPROACHING_QUOTES = listOf(
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & Öğle Vakti",
            title = "Gök Kapılarının Açıldığı An",
            text = "Öğle vakti, sema kapılarının açıldığı ve salih amellerin Allah Teâlâ'ya yükseldiği mübarek bir vakittir.",
            source = "Tirmizî, Vitir, 15 (Diyanet İşleri Başkanlığı)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & Günün Telaşı",
            title = "Dünya Meşgalesinde Tevekkül",
            text = "Günün ortasındaki dünya telaşından sıyrılıp Rabbin huzuruna durmak, kalbin yorgunluğunu dindirir ve ömrü bereketlendirir.",
            source = "Diyanet Hadislerle İslam",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet Meali · Âyet-i Kerime & Öğle",
            title = "Namaz Vakitli Bir Farzdır",
            arabic = "إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَوْقُوتًا",
            text = "Şüphesiz namaz, mü’minler üzerine belirli vakitlerde farz kılınmıştır. Vaktinde kılınan namaz Allah katında en sevimli ameldir.",
            source = "Nisâ Sûresi, 103. Âyet (Diyanet Meali)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Öğle Niyazı ve Dua",
            title = "Günün İkramı ve Şükür",
            text = "Allah'ım! Günün yarısına ulaştık, bize lütfettiğin nimetlerin şükrünü eda edebilmeyi ve huzurunda samimiyetle secdeye varmayı nasip eyle.",
            source = "Diyanet Manevi İltica",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & Öğle Sünneti",
            title = "Öğle Namazının Fazileti",
            text = "Öğle namazından önce kılınan dört rekât sünnet, sema kapılarının aralandığı kıymetli bir ibadettir.",
            source = "Tirmizî, Salât, 192 (Diyanet Hadislerle İslam)",
            isDiyanet = true
        )
    )

    private val IKINDI_APPROACHING_QUOTES = listOf(
        DailyQuote(
            category = "Diyanet Meali · Âyet-i Kerime & İkindi",
            title = "Orta Namaza Dikkat Edin",
            arabic = "حَافِظُوا عَلَى الصَّلَوَاتِ وَالصَّلَاةِ الْوُسْطَىٰ",
            text = "Namazlara ve orta namaza (ikindi namazına) devam edin; gönülden boyun eğerek Allah'ın huzurunda durun.",
            source = "Bakara Sûresi, 238. Âyet (Diyanet İşleri Başkanlığı Meali)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & İkindi Müjdesi",
            title = "Cennet Müjdesi",
            text = "Kim sabah ve ikindi namazlarını aksatmadan huşû ile kılarsa cennete girer.",
            source = "Buhârî, Mevâkît, 17; Müslim, Mesâcid, 215",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & İkindi Vaktinin Kıymeti",
            title = "Meleklerin Hüsn-ü Şahitliği",
            text = "İkindi namazını vaktinde kılan kimse, o vaktin meleklerinin Rablerine sunacağı güzel şahitliğe nail olur.",
            source = "Buhârî, Ezân, 31 (Diyanet Hadislerle İslam)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · İkindi Duası ve İltica",
            title = "Günün Hitamında İstiğfar",
            text = "Rabbimiz! Gün batımına doğru ilerlerken huzurundayız; günahlarımızı bağışla, kalan ömrümüzü ve amellerimizi rızanla taçlandır.",
            source = "Diyanet Manevi Niyaz",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Hikmet ve İbadet & İkindi",
            title = "Vaktin Kıymetini Bilmek",
            text = "Asr vaktinde ikindi namazı, zamanın hızla akıp gittiğini hatırlatan ve kulun Rabbine sadakatini tazeleyen mukaddes bir kalkandır.",
            source = "Diyanet Hadislerle İslam Tefekkürü",
            isDiyanet = true
        )
    )

    private val AKSAM_APPROACHING_QUOTES = listOf(
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & Akşam Niyazı",
            title = "Dua ve İltica Vakti",
            text = "Akşam vakti girdiğinde dualarınıza sarılın; günün yorgunluğunu huzur ve şükür secdesiyle taçlandırın.",
            source = "Ebû Dâvûd, Salât, 9 (Diyanet İşleri Başkanlığı)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & Akşam Ezanı Duası",
            title = "Akşam Ezanı İlticası",
            text = "Akşam ezanı okunurken: 'Allah'ım! Bu Senin gecenin gelişi, gündüzünün çekilişi ve kullarının davet sesleridir. Beni ve sevdiklerimi bağışla' diye dua ediniz.",
            source = "Tirmizî, De'avât, 85 (Diyanet İşleri Başkanlığı)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet Meali · Âyet-i Kerime & Akşam",
            title = "İyilikler Kötülükleri Giderir",
            arabic = "إِنَّ الْحَسَنَاتِ يُذْهِبْنَ السَّيِّئَاتِ",
            text = "Gündüzün iki tarafında ve gecenin gündüze yakın vakitlerinde namaz kıl. Çünkü iyilikler kötülükleri giderir.",
            source = "Hûd Sûresi, 114. Âyet (Diyanet Meali)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Akşam Duası ve İstiğfar",
            title = "Günün Şükrü ve Huzur",
            text = "Allah'ım! Akşama erdik, mülk de Sana ait olarak akşama erdi. Bizi bu gecenin şerrinden koru, hayrını ve affını üzerimize yağdır.",
            source = "Müslim, Zikir, 75 & Diyanet Niyaz",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & Akşam Namazı",
            title = "Akşam Namazının Bereketli Ecri",
            text = "Akşam namazından sonra yapılan samimi tevbe ve dualar, gün boyunca işlenen küçük kusurlara kefaret olur.",
            source = "Tirmizî, De'avât, 108 (Diyanet Hadislerle İslam)",
            isDiyanet = true
        )
    )

    private val YATSI_APPROACHING_QUOTES = listOf(
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & Yatsı ve Gece",
            title = "Yatsı Namazının Müjdesi",
            text = "Yatsı namazını cemaatle kılan kimse, gecenin yarısını ibadet ve namazla ihya etmiş gibi ecir kazanır.",
            source = "Müslim, Mesâcid, 260 (Diyanet İşleri Başkanlığı Hadislerle İslam)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Gece İbadeti & Vitir Bereketi",
            title = "Günü İbadetle Mühürlemek",
            text = "Günü yatsı ve vitir namazıyla tamamlamak, kabir gecesini aydınlatmanın ve huzurlu bir uykunun en yüce vesilesidir.",
            source = "Diyanet Hadislerle İslam",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet Meali · Âyet-i Kerime & Gece Secdesi",
            title = "Gece Vakti Huşû",
            arabic = "وَمِنَ اللَّيْلِ فَاسْجُدْ لَهُ وَسَبِّحْهُ لَيْلًا طَوِيلًا",
            text = "Gecenin bir kısmında da O'na secde et ve geceleyin O'nu uzun uzun tesbih et.",
            source = "İnsan Sûresi, 26. Âyet (Diyanet Meali)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Yatsı Duası ve Tevekkül",
            title = "Geceye Giriş Duası",
            text = "Allah'ım! Günün beş vaktini huzurunda tamamlamayı nasip eyledin, hamdolsun. Uykumuzu ibadet, uyanışımızı şükür ve afiyet kıl.",
            source = "Diyanet Manevi Niyaz",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Diyanet · Hadis-i Şerif & Vitir Namazı",
            title = "Vitir Namazının Önemi",
            text = "Şüphesiz Allah tektir (vitr), teki sever. Ey Kur'an ehli! Siz de vitir namazını kılınız.",
            source = "Tirmizî, Vitir, 2; Ebû Dâvûd, Vitir, 1",
            isDiyanet = true
        )
    )

    fun getPrayerApproachingQuote(prayerName: String, customDay: Int? = null): DailyQuote {
        val cleanName = prayerName.lowercase()
        val cal = Calendar.getInstance()
        val dayIndex = customDay ?: (cal.get(Calendar.DAY_OF_YEAR) + cal.get(Calendar.YEAR))
        return when {
            cleanName.contains("sabah") || cleanName.contains("imsak") -> {
                SABAH_APPROACHING_QUOTES[dayIndex % SABAH_APPROACHING_QUOTES.size]
            }
            cleanName.contains("cuma") -> {
                CUMA_APPROACHING_QUOTES[dayIndex % CUMA_APPROACHING_QUOTES.size]
            }
            cleanName.contains("öğle") || cleanName.contains("ogle") -> {
                OGLE_APPROACHING_QUOTES[dayIndex % OGLE_APPROACHING_QUOTES.size]
            }
            cleanName.contains("ikindi") -> {
                IKINDI_APPROACHING_QUOTES[dayIndex % IKINDI_APPROACHING_QUOTES.size]
            }
            cleanName.contains("akşam") || cleanName.contains("aksam") -> {
                AKSAM_APPROACHING_QUOTES[dayIndex % AKSAM_APPROACHING_QUOTES.size]
            }
            cleanName.contains("yatsı") || cleanName.contains("yatsi") -> {
                YATSI_APPROACHING_QUOTES[dayIndex % YATSI_APPROACHING_QUOTES.size]
            }
            else -> {
                getRandomRotatingNotificationQuote(customDay)
            }
        }
    }
}

