@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cscaprep.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cscaprep.app.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Screen { HOME, PREP, MOCKS, EXAM, RESULT, HISTORY, PROFILE, AUTH, VERIFY }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CSCAPrepApp(api: ApiClient, session: SessionStore) {
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(Screen.HOME) }
    var previous by remember { mutableStateOf(Screen.HOME) }
    var bootstrap by remember { mutableStateOf<Bootstrap?>(null) }
    var user by remember { mutableStateOf<User?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var prepQuestion by remember { mutableStateOf<Question?>(null) }
    var prepSubject by remember { mutableStateOf("mathematics") }
    var prepDifficulty by remember { mutableStateOf("standard") }
    var examContent by remember { mutableStateOf<ExamContent?>(null) }
    var result by remember { mutableStateOf<Result?>(null) }

    fun refresh() { scope.launch { loading = true; error = ""; try { if (session.loggedIn) user = api.me(); bootstrap = api.bootstrap() } catch (e: ApiException) { if (e.status == 401) { session.clear(); user = null; bootstrap = api.bootstrap() } else error = e.message.orEmpty() } catch (e: Exception) { error = e.message ?: "Could not connect to CSCAPrep." } finally { loading = false } } }
    LaunchedEffect(Unit) { refresh() }

    fun openAuth(from: Screen) { previous = from; screen = Screen.AUTH }
    fun backHome() { screen = Screen.HOME; prepQuestion = null; examContent = null }
    BackHandler(screen != Screen.HOME && screen != Screen.EXAM) { backHome() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            loading && bootstrap == null -> LoadingScreen()
            screen == Screen.AUTH -> AuthScreen(api, session, onBack = { screen = previous }, onAuthenticated = { account -> user = account; refresh(); screen = if (account.verified) previous else Screen.VERIFY })
            screen == Screen.VERIFY -> VerificationScreen(user ?: User(0, session.name, session.email, false, "free", session.language), onBack = { screen = Screen.HOME }, onRefresh = { scope.launch { try { val latest = api.me(); user = latest; if (latest.verified) { refresh(); screen = previous } } catch (e: Exception) { error = e.message.orEmpty() } } }, onResend = { scope.launch { try { api.resendVerification() } catch (e: Exception) { error = e.message.orEmpty() } } })
            screen == Screen.PREP -> PrepScreen(bootstrap, prepQuestion, prepSubject, prepDifficulty, error, loading, onBack = { backHome() }, onSubject = { prepSubject = it; prepQuestion = null }, onDifficulty = { prepDifficulty = it; prepQuestion = null }, onLoad = { scope.launch { loading = true; error = ""; try { prepQuestion = api.prep(prepSubject, prepDifficulty) } catch (e: ApiException) { error = e.message.orEmpty(); if (e.status == 401) openAuth(Screen.PREP) } finally { loading = false } } }, onNext = { scope.launch { loading = true; error = ""; try { prepQuestion = api.prep(prepSubject, prepDifficulty) } catch (e: ApiException) { error = e.message.orEmpty(); if (e.status == 401) openAuth(Screen.PREP) } finally { loading = false } } })
            screen == Screen.MOCKS -> MockLibrary(bootstrap, user, onBack = { backHome() }, onLogin = { openAuth(Screen.MOCKS) }, onExam = { exam, lang -> scope.launch { loading = true; error = ""; try { examContent = api.exam(exam.id, lang); screen = Screen.EXAM } catch (e: ApiException) { error = e.message.orEmpty(); if (e.status == 401 || e.status == 403) openAuth(Screen.MOCKS) } finally { loading = false } } })
            screen == Screen.EXAM && examContent != null -> ExamPlayer(examContent!!, onExit = { screen = Screen.MOCKS }, onSubmit = { answers, seconds -> scope.launch { loading = true; try { result = api.submit(examContent!!.exam.id, answers, seconds); refresh(); screen = Screen.RESULT } catch (e: Exception) { error = e.message ?: "Could not submit the exam." } finally { loading = false } } })
            screen == Screen.RESULT && result != null -> ResultScreen(result!!, examContent?.exam?.title.orEmpty(), onDone = { backHome() })
            screen == Screen.HISTORY -> HistoryScreen(bootstrap?.attempts.orEmpty(), onBack = { backHome() })
            screen == Screen.PROFILE -> ProfileScreen(user, bootstrap?.access, session.language, error, onBack = { backHome() }, onLogin = { openAuth(Screen.PROFILE) }, onVerify = { previous = Screen.PROFILE; screen = Screen.VERIFY }, onLanguage = { lang -> scope.launch { try { api.saveLanguage(lang); session.language = lang; refresh() } catch (e: Exception) { error = e.message.orEmpty() } } }, onLogout = { scope.launch { api.logout(); user = null; refresh(); screen = Screen.HOME } })
            else -> HomeScreen(bootstrap, user, error, onPrep = { screen = Screen.PREP }, onMock = { if (session.loggedIn && user?.verified == true) screen = Screen.MOCKS else openAuth(Screen.MOCKS) }, onHistory = { if (session.loggedIn && user?.verified == true) screen = Screen.HISTORY else openAuth(Screen.HISTORY) }, onProfile = { screen = Screen.PROFILE })
        }
    }
}

@Composable private fun LoadingScreen() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Spacer(Modifier.height(14.dp)); Text("Connecting to CSCAPrep…", color = Muted) } } }

@Composable private fun Brand(compact: Boolean = false) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(if (compact) 34.dp else 46.dp).background(Blue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("C", color = Color.White, fontSize = if (compact) 20.sp else 27.sp, fontWeight = FontWeight.Black) }; Spacer(Modifier.width(10.dp)); Column { Text("CSCAPrep.com", fontSize = if (compact) 19.sp else 25.sp, fontWeight = FontWeight.Bold, color = Navy); if (!compact) Text("Prepare with confidence", color = Muted, fontSize = 13.sp) } } }

@Composable private fun HomeScreen(data: Bootstrap?, user: User?, error: String, onPrep: () -> Unit, onMock: () -> Unit, onHistory: () -> Unit, onProfile: () -> Unit) {
    Scaffold(bottomBar = { NavigationBar { NavigationBarItem(true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }); NavigationBarItem(false, onClick = onHistory, icon = { Icon(Icons.Default.History, null) }, label = { Text("History") }); NavigationBarItem(false, onClick = onProfile, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile") }) } }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp), contentPadding = PaddingValues(vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Brand(); Spacer(Modifier.height(22.dp)); Text(if (user != null) "Welcome back, ${user.name}" else "Start preparing for the CSCA", fontSize = 27.sp, fontWeight = FontWeight.Bold); Text(if (user != null) "Continue your preparation or begin a realistic mock exam." else "Try five Prep questions before creating your account.", color = Muted, modifier = Modifier.padding(top = 6.dp)) }
            if (error.isNotBlank()) item { ErrorCard(error) }
            item { ModeCard(Icons.Default.AutoStories, "Prep Mode", "Learn one question at a time with immediate answers and explanations.", "Start practicing", Blue, onPrep) }
            item { ModeCard(Icons.Default.Timer, "Mock Exam", "A timed, full-screen exam with answer sheet, flags and final submission.", "Open mock exams", Navy, onMock) }
            item { Text("Five CSCA subjects", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            items(data?.subjects.orEmpty()) { s -> Card { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(46.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) { Text(s.symbol, color = Blue, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(s.label, fontWeight = FontWeight.SemiBold); Text("${s.questions} questions • ${s.minutes} minutes", color = Muted, fontSize = 13.sp) } } } }
        }
    }
}

@Composable private fun ModeCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, action: String, color: Color, onClick: () -> Unit) { Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(20.dp)) { Icon(icon, null, tint = color, modifier = Modifier.size(34.dp)); Spacer(Modifier.height(12.dp)); Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text(description, color = Muted, modifier = Modifier.padding(vertical = 8.dp)); Button(onClick, colors = ButtonDefaults.buttonColors(containerColor = color), modifier = Modifier.fillMaxWidth()) { Text(action) } } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PrepScreen(data: Bootstrap?, question: Question?, subject: String, difficulty: String, error: String, loading: Boolean, onBack: () -> Unit, onSubject: (String) -> Unit, onDifficulty: (String) -> Unit, onLoad: () -> Unit, onNext: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Prep Mode") }, navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(18.dp).verticalScroll(rememberScrollState())) {
            if (question == null) {
                Text("Choose a subject", fontSize = 22.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp))
                data?.subjects?.forEach { s -> FilterChip(selected = subject == s.id, onClick = { onSubject(s.id) }, label = { Text(s.label) }, leadingIcon = { Text(s.symbol) }, modifier = Modifier.padding(end = 8.dp, bottom = 6.dp)) }
                Spacer(Modifier.height(14.dp)); Text("Difficulty", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("easy", "standard", "hard").forEach { d -> FilterChip(difficulty == d, { onDifficulty(d) }, { Text(d.replaceFirstChar { it.uppercase() }) }) } }
                if (error.isNotBlank()) ErrorCard(error)
                Spacer(Modifier.height(20.dp)); Button(onLoad, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp)) { if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Get question") }
            } else { QuestionCard(question, true, onNext) }
        }
    }
}

@Composable private fun QuestionCard(question: Question, revealAllowed: Boolean, onNext: () -> Unit) {
    var selected by remember(question.id) { mutableStateOf<Int?>(null) }; var revealed by remember(question.id) { mutableStateOf(false) }
    Text(question.topic.ifBlank { question.subject.replace('-', ' ').replaceFirstChar { it.uppercase() } }, color = Blue, fontWeight = FontWeight.Bold)
    Text(question.prompt, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 18.dp))
    question.options.forEachIndexed { index, option -> val correct = revealed && question.answer == index; val wrong = revealed && selected == index && question.answer != index; val border = when { correct -> Green; wrong -> MaterialTheme.colorScheme.error; selected == index -> Blue; else -> MaterialTheme.colorScheme.outline }; Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).border(1.5.dp, border, RoundedCornerShape(14.dp)).clickable(enabled = !revealed) { selected = index }) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(('A'.code + index).toChar().toString(), fontWeight = FontWeight.Bold, color = border); Spacer(Modifier.width(14.dp)); Text(option, Modifier.weight(1f)) } } }
    if (!revealed) Button({ if (selected != null && revealAllowed) revealed = true }, enabled = selected != null, modifier = Modifier.fillMaxWidth()) { Text("Check answer") }
    else { Card(colors = CardDefaults.cardColors(containerColor = if (selected == question.answer) Color(0xFFEAF7EF) else Color(0xFFFFF1EF))) { Column(Modifier.padding(16.dp)) { Text(if (selected == question.answer) "Correct" else "Not quite", color = if (selected == question.answer) Green else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 18.sp); if (question.principle.isNotBlank()) Text(question.principle, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp)); Text(question.explanation, modifier = Modifier.padding(top = 8.dp)) } }; Spacer(Modifier.height(14.dp)); Button(onNext, Modifier.fillMaxWidth()) { Text("Next question") } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun MockLibrary(data: Bootstrap?, user: User?, onBack: () -> Unit, onLogin: () -> Unit, onExam: (Exam, String) -> Unit) {
    var language by remember { mutableStateOf("en") }
    Scaffold(topBar = { TopAppBar(title = { Text("Mock Exams") }, navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 18.dp), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (user == null || !user.verified) item { ErrorCard("Log in and verify your email before starting a Mock Exam."); Button(onLogin, Modifier.fillMaxWidth()) { Text("Log in or sign up") } }
            else { item { Text("Exam language", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(language == "en", { language = "en" }, { Text("English") }); FilterChip(language == "zh", { language = "zh" }, { Text("中文") }) }; Text("Chinese subjects always remain in Chinese.", color = Muted, fontSize = 13.sp) }
                items(data?.exams.orEmpty()) { exam -> Card(Modifier.fillMaxWidth().clickable { onExam(exam, if (exam.subject.endsWith("chinese")) "zh" else language) }) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(exam.title, fontWeight = FontWeight.Bold); Text("${exam.duration} minutes • ${exam.count} questions", color = Muted) }; Icon(Icons.Default.ChevronRight, null) } } }
            }
        }
    }
}

@Composable private fun ExamPlayer(content: ExamContent, onExit: () -> Unit, onSubmit: (Map<Long, Int>, Int) -> Unit) {
    var index by remember { mutableIntStateOf(0) }; val answers = remember { mutableStateMapOf<Long, Int>() }; val flags = remember { mutableStateMapOf<Long, Boolean>() }; var secondsLeft by remember { mutableIntStateOf(content.exam.duration * 60) }; var showSheet by remember { mutableStateOf(false) }; var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (secondsLeft > 0) { delay(1000); secondsLeft-- }; onSubmit(answers.toMap(), content.exam.duration * 60) }
    BackHandler { confirm = true }
    val q = content.questions[index]; val elapsed = content.exam.duration * 60 - secondsLeft
    Scaffold(topBar = { Row(Modifier.fillMaxWidth().background(Navy).statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { TextButton({ confirm = true }) { Text("Exit", color = Color.White) }; Text(content.exam.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f)); Icon(Icons.Default.Timer, null, tint = Color.White); Spacer(Modifier.width(5.dp)); Text("%02d:%02d".format(secondsLeft / 60, secondsLeft % 60), color = if (secondsLeft < 300) Color(0xFFFFC7C2) else Color.White, fontWeight = FontWeight.Bold) } }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(10.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedButton({ showSheet = true }) { Icon(Icons.Default.GridView, null); Spacer(Modifier.width(6.dp)); Text("Answer sheet") }; Spacer(Modifier.weight(1f)); Text("${index + 1} / ${content.questions.size}", fontWeight = FontWeight.Bold); IconButton({ flags[q.id] = !(flags[q.id] ?: false) }) { Icon(if (flags[q.id] == true) Icons.Default.Flag else Icons.Default.OutlinedFlag, null, tint = if (flags[q.id] == true) Amber else Muted) } }
            Column(Modifier.weight(1f).padding(18.dp).verticalScroll(rememberScrollState())) { Text(q.topic, color = Blue, fontWeight = FontWeight.Bold); Text(q.prompt, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 16.dp)); q.options.forEachIndexed { option, text -> Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).border(1.5.dp, if (answers[q.id] == option) Blue else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).clickable { answers[q.id] = option }) { Row(Modifier.padding(15.dp)) { RadioButton(answers[q.id] == option, { answers[q.id] = option }); Spacer(Modifier.width(8.dp)); Text(text, Modifier.weight(1f).align(Alignment.CenterVertically)) } } } }
            Row(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ if (index > 0) index-- }, enabled = index > 0, modifier = Modifier.weight(1f)) { Text("Previous") }; Button({ if (index < content.questions.lastIndex) index++ else confirm = true }, modifier = Modifier.weight(1f)) { Text(if (index == content.questions.lastIndex) "Submit" else "Next") } }
        }
    }
    if (showSheet) ModalBottomSheet({ showSheet = false }) { Text("Answer sheet", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp)); LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(16.dp)) { itemsIndexed(content.questions.chunked(6)) { row, list -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { list.forEachIndexed { col, item -> val n = row * 6 + col; val color = when { n == index -> Blue; flags[item.id] == true -> Amber; answers.containsKey(item.id) -> Green; else -> Color(0xFFD7E1E8) }; Box(Modifier.padding(5.dp).size(44.dp).background(color, CircleShape).clickable { index = n; showSheet = false }, contentAlignment = Alignment.Center) { Text("${n + 1}", color = if (color == Color(0xFFD7E1E8)) Navy else Color.White, fontWeight = FontWeight.Bold) } } } } }; Spacer(Modifier.height(24.dp)) }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Submit exam?") }, text = { Text("${content.questions.size - answers.size} unanswered and ${flags.count { it.value }} flagged. You cannot change answers after submission.") }, confirmButton = { Button({ confirm = false; onSubmit(answers.toMap(), elapsed) }) { Text("Submit exam") } }, dismissButton = { TextButton({ confirm = false }) { Text("Continue exam") } })
}

@Composable private fun ResultScreen(result: Result, title: String, onDone: () -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.EmojiEvents, null, tint = Amber, modifier = Modifier.size(64.dp)); Text(title, color = Muted, textAlign = TextAlign.Center); Text("${result.percent}%", fontSize = 58.sp, fontWeight = FontWeight.Black, color = Blue); Text("${result.score} of ${result.total} correct", fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(20.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { Stat("Correct", result.correct, Green); Stat("Incorrect", result.incorrect, MaterialTheme.colorScheme.error); Stat("Unanswered", result.unanswered, Muted) }; Spacer(Modifier.height(28.dp)); Button(onDone, Modifier.fillMaxWidth()) { Text("Return home") } }
}
@Composable private fun Stat(label: String, value: Int, color: Color) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$value", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color); Text(label, color = Muted, fontSize = 12.sp) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun HistoryScreen(attempts: List<Attempt>, onBack: () -> Unit) { Scaffold(topBar = { TopAppBar(title = { Text("History") }, navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad -> LazyColumn(Modifier.fillMaxSize().padding(pad).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { if (attempts.isEmpty()) item { Text("No completed Mock Exams yet.", color = Muted) }; items(attempts) { a -> Card { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(a.title, fontWeight = FontWeight.Bold); Text(a.createdAt, color = Muted, fontSize = 12.sp) }; Text(if (a.total > 0) "${a.score * 100 / a.total}%" else "—", color = Blue, fontSize = 22.sp, fontWeight = FontWeight.Bold) } } } } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ProfileScreen(user: User?, access: Access?, language: String, error: String, onBack: () -> Unit, onLogin: () -> Unit, onVerify: () -> Unit, onLanguage: (String) -> Unit, onLogout: () -> Unit) { Scaffold(topBar = { TopAppBar(title = { Text("Profile") }, navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad -> Column(Modifier.fillMaxSize().padding(pad).padding(20.dp).verticalScroll(rememberScrollState())) { Brand(); Spacer(Modifier.height(24.dp)); if (user == null) { Text("You are using the five-question guest preview.", color = Muted); Spacer(Modifier.height(12.dp)); Button(onLogin, Modifier.fillMaxWidth()) { Text("Log in or sign up") } } else { Text(user.name, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text(user.email, color = Muted); Text(if (user.verified) "Email verified" else "Email verification required", color = if (user.verified) Green else Amber, modifier = Modifier.padding(top = 6.dp)); if (!user.verified) Button(onVerify, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Verify email") }; Text("Plan: ${access?.plan ?: user.plan}", modifier = Modifier.padding(top = 18.dp), fontWeight = FontWeight.Bold) }; HorizontalDivider(Modifier.padding(vertical = 22.dp)); Text("Study language", fontWeight = FontWeight.Bold); FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("en" to "English", "zh" to "中文", "ar" to "العربية", "fr" to "Français", "es" to "Español").forEach { (code, label) -> FilterChip(language == code, { onLanguage(code) }, { Text(label) }) } }; Text("Math, Physics and Chemistry Prep follow this language. Chinese subjects stay in Chinese.", color = Muted, fontSize = 13.sp); if (error.isNotBlank()) ErrorCard(error); if (user != null) { Spacer(Modifier.height(28.dp)); OutlinedButton(onLogout, Modifier.fillMaxWidth()) { Text("Log out") } } } } }

@Composable private fun AuthScreen(api: ApiClient, session: SessionStore, onBack: () -> Unit, onAuthenticated: (User) -> Unit) { val scope = rememberCoroutineScope(); var register by remember { mutableStateOf(false) }; var forgot by remember { mutableStateOf(false) }; var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var loading by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }; var notice by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(22.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) }; Brand(); Spacer(Modifier.height(28.dp)); Text(when { forgot -> "Reset password"; register -> "Create your account"; else -> "Log in" }, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text(if (register) "Create an account after your free Prep preview." else "Continue your CSCA preparation.", color = Muted, modifier = Modifier.padding(vertical = 8.dp)); if (register) { OutlinedTextField(name, { name = it }, label = { Text("First name") }, singleLine = true, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(10.dp)) }; OutlinedTextField(email, { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, modifier = Modifier.fillMaxWidth()); if (!forgot) { Spacer(Modifier.height(10.dp)); OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, supportingText = { if (register) Text("At least 8 characters") }, modifier = Modifier.fillMaxWidth()) }; if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp)); if (notice.isNotBlank()) Text(notice, color = Green, modifier = Modifier.padding(top = 10.dp)); Spacer(Modifier.height(16.dp)); Button(onClick = { scope.launch { loading = true; error = ""; try { if (forgot) { api.forgotPassword(email); notice = "If an account exists, a reset email has been sent." } else { val auth = if (register) api.register(name, email, password, session.language) else api.login(email, password); session.save(auth); onAuthenticated(auth.user) } } catch (e: Exception) { error = e.message ?: "Authentication failed." } finally { loading = false } } }, enabled = !loading && email.isNotBlank() && (forgot || password.length >= if (register) 8 else 1), modifier = Modifier.fillMaxWidth().height(52.dp)) { if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (forgot) "Send reset email" else if (register) "Create account" else "Log in") }; if (!forgot) TextButton({ register = !register; error = "" }, Modifier.align(Alignment.CenterHorizontally)) { Text(if (register) "Already registered? Log in" else "New student? Sign up") }; TextButton({ forgot = !forgot; error = "" }, Modifier.align(Alignment.CenterHorizontally)) { Text(if (forgot) "Back to login" else "Forgot password?") }; Text("Native secure sign-in. The website is not embedded in this app.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) }
}

@Composable private fun VerificationScreen(user: User, onBack: () -> Unit, onRefresh: () -> Unit, onResend: () -> Unit) { Column(Modifier.fillMaxSize().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.MarkEmailUnread, null, tint = Blue, modifier = Modifier.size(70.dp)); Spacer(Modifier.height(18.dp)); Text("Verify your email", fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("We sent a secure link to ${user.email}. Open it, then return here.", color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp)); Button(onRefresh, Modifier.fillMaxWidth()) { Text("I verified my email") }; OutlinedButton(onResend, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Resend verification") }; TextButton(onBack) { Text("Back") } }
}

@Composable private fun ErrorCard(message: String) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) { Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp)) } }
