package com.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ChessArrowsApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChessArrowsApp() {
    var isArrowsEnabled by remember { mutableStateOf(true) }
    var selectedColor by remember { mutableStateOf("green") }
    var showHelpDialog by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }
    var pageProgress by remember { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }

    // Handle Android system back presses to navigate backwards in Chess.com reactively
    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }

    // Colors dictionary
    val colorsList = listOf(
        ColorItem("green", Color(0xFF4CAF50), "Green"),
        ColorItem("blue", Color(0xFF2196F3), "Blue"),
        ColorItem("red", Color(0xFFE53935), "Red"),
        ColorItem("orange", Color(0xFFFF9800), "Orange")
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12)),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Chess Arrows ♟️🏹",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isArrowsEnabled) "Swipe to draw native arrows" else "Arrows disabled",
                            fontSize = 11.sp,
                            color = if (isArrowsEnabled) Color(0xFF81C784) else Color(0xFFE57373)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Show instructions",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = {
                        webViewInstance?.reload()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh board",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF16161A),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color(0xFF16161A),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Nav control buttons (Back & Home)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            enabled = canGoBack,
                            onClick = { webViewInstance?.goBack() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go Back",
                                tint = if (canGoBack) Color.White else Color(0xFF55555C)
                            )
                        }
                        IconButton(
                            onClick = { webViewInstance?.loadUrl("https://www.chess.com/play/online") },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Chess Home",
                                tint = Color.White
                            )
                        }
                    }

                    // Middle: Enable Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Switch(
                            checked = isArrowsEnabled,
                            onCheckedChange = {
                                isArrowsEnabled = it
                                webViewInstance?.evaluateJavascript("javascript:if(window.setTouchArrowEnabled){ window.setTouchArrowEnabled($it); }", null)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFFFFF),
                                checkedTrackColor = Color(0xFF4CAF50),
                                uncheckedThumbColor = Color(0xFF90A4AE),
                                uncheckedTrackColor = Color(0xFF37474F)
                            ),
                            modifier = Modifier.scale(0.8f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Arrows",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Right: Color selectors + Trash cleaner
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorsList.forEach { colorItem ->
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(colorItem.color)
                                    .border(
                                        width = if (selectedColor == colorItem.id) 1.5.dp else 0.dp,
                                        color = if (selectedColor == colorItem.id) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedColor = colorItem.id
                                        webViewInstance?.evaluateJavascript(
                                            "javascript:if(window.setArrowColor){ window.setArrowColor('${colorItem.id}'); }",
                                            null
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == colorItem.id) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                webViewInstance?.evaluateJavascript("javascript:if(window.clearAllArrows){ window.clearAllArrows(); }", null)
                            },
                            modifier = Modifier
                                .background(Color(0xFF2C2C35), RoundedCornerShape(8.dp))
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear arrows",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF0F0F12))
        ) {
            // Main Web Browser Container loading chess.com
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F0F12)),
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            
                            // Emulate standard modern Android browser/Chrome representation
                            userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Mobile Safari/537.36"
                        }

                        // Allow session state cookies
                        CookieManager.getInstance().setAcceptCookie(true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                super.onPageFinished(view, url)
                                isLoadingPage = false
                                canGoBack = view.canGoBack()
                                // Auto inject customized gesture handler
                                injectArrowHelper(view, isArrowsEnabled, selectedColor)
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                canGoBack = view?.canGoBack() ?: false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                pageProgress = newProgress
                                if (newProgress == 100) {
                                    isLoadingPage = false
                                }
                            }
                        }

                        // Load playable Chess.com URL directly
                        loadUrl("https://www.chess.com/play/online")
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    // Keep JS synchronised with Compose interactive choices
                    webView.evaluateJavascript("javascript:if(window.setTouchArrowEnabled){ window.setTouchArrowEnabled($isArrowsEnabled); }", null)
                    webView.evaluateJavascript("javascript:if(window.setArrowColor){ window.setArrowColor('$selectedColor'); }", null)
                }
            )

            // Dynamic Progress Bar while loading the heavy website
            if (isLoadingPage) {
                LinearProgressIndicator(
                    progress = { pageProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter),
                    color = Color(0xFF81C784),
                    trackColor = Color(0xFF1E1E24)
                )
            }
        }

        // Help Instructions Dialog Explaining custom interactions
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = {
                    Text(
                        text = "Interactive Guide 💡",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "We have mapped touchscreen swipes directly to Chess.com's native desktop arrow engine!",
                            fontSize = 14.sp,
                            color = Color(0xFFB0BEC5)
                        )

                        HorizontalDivider(color = Color(0xFF37474F))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            InstructionRow(
                                title = "1️⃣ Draw Native Arrows",
                                text = "Swipe quickly from any board square to another. This instantly triggers Chess.com to draw an arrow in your selected color (Green, Blue, Red, or Orange)."
                            )

                            InstructionRow(
                                title = "2️⃣ Toggle Highlights",
                                text = "Tap and release quickly on a single square without dragging. This toggles a Chess.com native circular highlight."
                            )

                            InstructionRow(
                                title = "3️⃣ Move Chess Pieces",
                                text = "To move pieces, either tap a piece and then tap its destination, or HOLD a piece for 220ms (until you feel a short haptic bump) and swipe to drag-move it."
                            )

                            InstructionRow(
                                title = "4️⃣ Clear Arrows",
                                text = "Simply tap anywhere elements normally to clear all arrows, or use the red Trash/Clear button in the bottom controller bar."
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showHelpDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Get Started", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E1E24),
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun InstructionRow(title: String, text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFFCFD8DC),
            lineHeight = 16.sp
        )
    }
}

data class ColorItem(val id: String, val color: Color, val name: String)

// Simple scale modifier extension
fun Modifier.scale(scale: Float): Modifier = this.then(
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(
            (placeable.width * scale).toInt(),
            (placeable.height * scale).toInt()
        ) {
            placeable.placeRelative(0, 0)
        }
    }
)

/**
 * Clean JavaScript interface programmatically injected dynamically inside Chess.com container web document.
 * Emulates native desktop pointer mouse events (button: 2 right click) with key modifiers on swipes
 * to trigger Chess.com native arrows. Allows selecting Green, Blue, Red and Orange.
 */
fun injectArrowHelper(webView: WebView, isEnabled: Boolean, currentColor: String) {
    val jsCode = """
    (function() {
        if (window.__chess_arrow_helper_loaded) {
            console.log("Chess helper already installed; updating configuration.");
            if (window.setTouchArrowEnabled) window.setTouchArrowEnabled($isEnabled);
            if (window.setArrowColor) window.setArrowColor('$currentColor');
            return;
        }
        window.__chess_arrow_helper_loaded = true;

        console.log('Chess Custom Native Arrow Helper loaded!');

        let boardElement = null;
        let activeColorId = '$currentColor';
        let isPotentialHold = false;
        let isDrawingArrow = false;
        let isDraggingPiece = false;
        let touchStartX = 0;
        let touchStartY = 0;
        let startElement = null;
        let holdTimer = null;
        const HOLD_DELAY = 220; // 220ms hold threshold for moves
        let isTouchArrowEnabled = $isEnabled;

        // Modifier key mappings for chess.com native arrows:
        // - Default (Orange/Red): Ctrl/Alt/Shift combined logic, standard right-drag is default orange/red
        // - Green: Shift=true
        // - Blue: Alt=true
        // - Red: Ctrl=true
        // - Orange: metaKey or no key depending on default
        function getModifiers(colorId) {
            let modifiers = { shiftKey: false, altKey: false, ctrlKey: false, metaKey: false };
            if (colorId === 'green') {
                modifiers.shiftKey = true;
            } else if (colorId === 'blue') {
                modifiers.altKey = true;
            } else if (colorId === 'red') {
                modifiers.ctrlKey = true;
            } else if (colorId === 'orange') {
                // Orange handles default right click colors
            }
            return modifiers;
        }

        window.setArrowColor = function(colorId) {
            activeColorId = colorId;
        };

        window.setTouchArrowEnabled = function(enabled) {
            isTouchArrowEnabled = !!enabled;
        };

        window.clearAllArrows = function() {
            if (!boardElement) return;
            const rect = boardElement.getBoundingClientRect();
            const clientX = rect.left + rect.width / 2;
            const clientY = rect.top + rect.height / 2;
            const target = document.elementFromPoint(clientX, clientY) || boardElement;
            
            const init = {
                bubbles: true,
                cancelable: true,
                view: window,
                button: 0,
                buttons: 0,
                clientX: clientX,
                clientY: clientY
            };
            target.dispatchEvent(new MouseEvent('mousedown', init));
            target.dispatchEvent(new MouseEvent('mouseup', init));
        };

        function dispatchRightClickEvent(type, clientX, clientY, target) {
            if (!target) return;
            const modifiers = getModifiers(activeColorId);
            
            const init = {
                bubbles: true,
                cancelable: true,
                view: window,
                button: 2, // Right click
                buttons: type === 'mouseup' ? 0 : 2,
                which: 3,
                clientX: clientX,
                clientY: clientY,
                screenX: clientX,
                screenY: clientY,
                shiftKey: modifiers.shiftKey,
                altKey: modifiers.altKey,
                ctrlKey: modifiers.ctrlKey,
                metaKey: modifiers.metaKey
            };

            // Dispatch PointerEvent first for modern board frameworks
            try {
                const pType = type === 'mousedown' ? 'pointerdown' : (type === 'mouseup' ? 'pointerup' : 'pointermove');
                const pInit = Object.assign({}, init, {
                    pointerId: 1,
                    pointerType: 'mouse',
                    isPrimary: true,
                    pressure: type === 'mouseup' ? 0 : 0.5
                });
                const pEvt = new PointerEvent(pType, pInit);
                target.dispatchEvent(pEvt);
            } catch(e) {}

            // Dispatch MouseEvent fallback
            try {
                const mEvt = new MouseEvent(type, init);
                target.dispatchEvent(mEvt);
            } catch(e) {}
        }

        function handleTouchStart(event) {
            if (!isTouchArrowEnabled) return;
            if (event.touches.length !== 1) return;

            const touch = event.touches[0];
            const rect = boardElement.getBoundingClientRect();
            const rx = (touch.clientX - rect.left) / rect.width;
            const ry = (touch.clientY - rect.top) / rect.height;

            if (rx < 0 || rx > 1 || ry < 0 || ry > 1) return;

            touchStartX = touch.clientX;
            touchStartY = touch.clientY;
            startElement = document.elementFromPoint(touch.clientX, touch.clientY) || boardElement;

            isPotentialHold = true;
            isDrawingArrow = false;
            isDraggingPiece = false;

            clearTimeout(holdTimer);
            holdTimer = setTimeout(() => {
                if (isPotentialHold) {
                    isPotentialHold = false;
                    isDraggingPiece = true;
                    if (navigator.vibrate) {
                        navigator.vibrate(12);
                    }
                }
            }, HOLD_DELAY);
        }

        function handleTouchMove(event) {
            if (!isTouchArrowEnabled) return;
            if (event.touches.length !== 1) return;

            const touch = event.touches[0];
            const dx = touch.clientX - touchStartX;
            const dy = touch.clientY - touchStartY;
            const dist = Math.hypot(dx, dy);

            if (isPotentialHold && dist > 12) {
                clearTimeout(holdTimer);
                isPotentialHold = false;
                isDrawingArrow = true;

                // Fire native down event for drawing right-click arrow helper
                dispatchRightClickEvent('mousedown', touchStartX, touchStartY, startElement);
            }

            if (isDrawingArrow) {
                event.preventDefault(); // Stop scrolling/resizing or standard drag-pieces
                event.stopPropagation();

                const target = document.elementFromPoint(touch.clientX, touch.clientY) || boardElement;
                dispatchRightClickEvent('mousemove', touch.clientX, touch.clientY, target);
            } else if (!isDraggingPiece) {
                // Prevent moving standard chess pieces before we confirm the user is holding
                event.preventDefault();
                event.stopPropagation();
            }
        }

        function handleTouchEnd(event) {
            clearTimeout(holdTimer);

            if (isDrawingArrow) {
                event.preventDefault();
                event.stopPropagation();

                const touch = event.changedTouches[0];
                const target = document.elementFromPoint(touch.clientX, touch.clientY) || boardElement;
                dispatchRightClickEvent('mouseup', touch.clientX, touch.clientY, target);
                isDrawingArrow = false;
            } else if (isDraggingPiece) {
                isDraggingPiece = false;
            } else {
                // Normal click handles selection on Chess.com
            }
        }

        function handleTouchCancel(event) {
            clearTimeout(holdTimer);
            if (isDrawingArrow) {
                const touch = event.changedTouches[0] || { clientX: touchStartX, clientY: touchStartY };
                const target = boardElement;
                dispatchRightClickEvent('mouseup', touch.clientX, touch.clientY, target);
            }
            isDrawingArrow = false;
            isDraggingPiece = false;
        }

        function setupTouchListeners(board) {
            if (board.__arrowsHooked) return;
            board.__arrowsHooked = true;

            board.addEventListener('touchstart', handleTouchStart, { capture: true, passive: false });
            board.addEventListener('touchmove', handleTouchMove, { capture: true, passive: false });
            board.addEventListener('touchend', handleTouchEnd, { capture: true, passive: false });
            board.addEventListener('touchcancel', handleTouchCancel, { capture: true, passive: false });
        }

        function findAndHookBoard() {
            const board = document.querySelector('chess-board, .board, #board, [id*="board"], [class*="board"]');
            if (!board) {
                boardElement = null;
                return;
            }

            if (boardElement !== board) {
                boardElement = board;
                setupTouchListeners(board);
                console.log("Hooked chess board successfully.");
            }
        }

        // Keep checking for the board (as we might render/re-render boards as games start/end)
        setInterval(findAndHookBoard, 1000);
    })();
    """.trimIndent()

    webView.evaluateJavascript(jsCode, null)
}
