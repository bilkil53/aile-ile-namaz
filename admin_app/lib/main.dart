import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

const _url = String.fromEnvironment('SUPABASE_URL');
const _key = String.fromEnvironment('SUPABASE_ANON_KEY');

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Supabase.initialize(url: _url, anonKey: _key);
  runApp(const YonetimApp());
}

SupabaseClient get _db => Supabase.instance.client;

class YonetimApp extends StatelessWidget {
  const YonetimApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'Lisans Yönetimi',
        theme: ThemeData(colorSchemeSeed: Colors.teal, useMaterial3: true),
        home: StreamBuilder<AuthState>(
          stream: _db.auth.onAuthStateChange,
          builder: (_, __) => _db.auth.currentSession == null ? const LoginPage() : const LicensesPage(),
        ),
      );
}

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});
  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _email = TextEditingController();
  final _pass = TextEditingController();
  String? _err;
  bool _busy = false;

  Future<void> _login() async {
    setState(() { _busy = true; _err = null; });
    try {
      await _db.auth.signInWithPassword(email: _email.text.trim(), password: _pass.text);
    } on AuthException catch (e) {
      setState(() => _err = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Yönetici girişi')),
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(children: [
            TextField(controller: _email, decoration: const InputDecoration(labelText: 'E-posta'), keyboardType: TextInputType.emailAddress),
            TextField(controller: _pass, decoration: const InputDecoration(labelText: 'Parola'), obscureText: true),
            if (_err != null) Padding(padding: const EdgeInsets.only(top: 8), child: Text(_err!, style: const TextStyle(color: Colors.red))),
            const SizedBox(height: 16),
            FilledButton(onPressed: _busy ? null : _login, child: const Text('Giriş')),
          ]),
        ),
      );
}

class LicensesPage extends StatefulWidget {
  const LicensesPage({super.key});
  @override
  State<LicensesPage> createState() => _LicensesPageState();
}

class _LicensesPageState extends State<LicensesPage> {
  List<Map<String, dynamic>> _rows = [];
  String _q = '';
  String? _err;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _loading = true; _err = null; });
    try {
      final r = await _db.rpc('admin_list_licenses') as List;
      _rows = r.cast<Map<String, dynamic>>();
    } on PostgrestException catch (e) {
      _err = e.code == '42501' ? 'Bu hesap yönetici değil.' : e.message;
    }
    if (mounted) setState(() => _loading = false);
  }

  static String _date(dynamic v) {
    if (v == null) return '–';
    final d = DateTime.parse(v as String).toLocal();
    return '${d.day.toString().padLeft(2, '0')}.${d.month.toString().padLeft(2, '0')}.${d.year}';
  }

  static const _planName = {'3_ay': '3 aylık', '12_ay': 'Yıllık'};

  Future<void> _give(Map<String, dynamic> r, String plan) async {
    try {
      await _db.rpc('admin_set_license', params: {'p_uid': r['user_id'], 'p_plan': plan});
      await _load();
    } on PostgrestException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  Future<void> _revoke(Map<String, dynamic> r) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('Lisans kaldırılsın mı?'),
        content: Text('${r['email']}'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Vazgeç')),
          FilledButton(onPressed: () => Navigator.pop(c, true), child: const Text('Kaldır')),
        ],
      ),
    );
    if (ok != true) return;
    await _db.rpc('admin_revoke_license', params: {'p_uid': r['user_id']});
    await _load();
  }

  @override
  Widget build(BuildContext context) {
    final q = _q.toLowerCase();
    final rows = _rows.where((r) => '${r['email']} ${r['display_name']}'.toLowerCase().contains(q)).toList();
    final pro = _rows.where((r) => r['status'] == 'pro').length;
    return Scaffold(
      appBar: AppBar(
        title: Text('Lisanslar ($pro Pro / ${_rows.length})'),
        actions: [
          IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
          IconButton(onPressed: () => _db.auth.signOut(), icon: const Icon(Icons.logout)),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _err != null
              ? Center(child: Text(_err!))
              : Column(children: [
                  Padding(
                    padding: const EdgeInsets.all(8),
                    child: TextField(
                      decoration: const InputDecoration(prefixIcon: Icon(Icons.search), hintText: 'E-posta ya da ad ara'),
                      onChanged: (v) => setState(() => _q = v),
                    ),
                  ),
                  Expanded(
                    child: ListView.separated(
                      itemCount: rows.length,
                      separatorBuilder: (_, __) => const Divider(height: 1),
                      itemBuilder: (_, i) {
                        final r = rows[i];
                        final st = {'pro': 'Pro', 'deneme': 'Deneme', 'ucretsiz': 'Ücretsiz'}[r['status']] ?? '';
                        return ListTile(
                          title: Text('${r['email'] ?? '-'}'),
                          subtitle: Text('${r['display_name'] ?? ''}\n$st · ${_planName[r['plan']] ?? '–'} · Pro bitiş: ${_date(r['paid_until'])} · Deneme: ${_date(r['trial_until'])} · Kayıt: ${_date(r['created_at'])}'),
                          isThreeLine: true,
                          trailing: PopupMenuButton<String>(
                            onSelected: (v) => v == 'sil' ? _revoke(r) : _give(r, v),
                            itemBuilder: (_) => const [
                              PopupMenuItem(value: '3_ay', child: Text('3 ay ekle')),
                              PopupMenuItem(value: '12_ay', child: Text('12 ay ekle')),
                              PopupMenuItem(value: 'sil', child: Text('Lisansı kaldır')),
                            ],
                          ),
                        );
                      },
                    ),
                  ),
                ]),
    );
  }
}
