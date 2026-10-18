    private fun showAiMenu() {
        if (menuView != null) return
        val wm = windowManager ?: return
        val petParams = layoutParams ?: return
        val pet = petView ?: return

        // 1) 菜单视图（与原来一致）
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xE6FFFFFF.toInt())  // 半透明白底
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        menu.addView(makeMenuItem(getString(R.string.ai_chatgpt)) {
            openPetApp(CHATGPT_PACKAGE, getString(R.string.ai_chatgpt))
        })
        menu.addView(makeMenuItem(getString(R.string.ai_gemini)) {
            openPetApp(GEMINI_PACKAGE, getString(R.string.ai_gemini))
        })
        menu.addView(makeMenuItem(getString(R.string.ai_dshbox)) {
            openPetApp(DSH_PACKAGE, getString(R.string.ai_dshbox))
        })

        // 2) 实测菜单与小猫尺寸（不假设固定宽度）
        val unbound = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        menu.measure(unbound, unbound)
        val menuW = menu.measuredWidth
        val menuH = menu.measuredHeight
        if (pet.width <= 0 || pet.height <= 0) {
            pet.measure(unbound, unbound)
        }
        val petW = if (pet.width > 0) pet.width else pet.measuredWidth
        val petH = if (pet.height > 0) pet.height else pet.measuredHeight

        // 3) 可用区域：左右 0..screenW，上边界避开状态栏，下边界到屏幕底
        val dm = resources.displayMetrics
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels
        val statusBarRes = resources.getIdentifier("status_bar_height", "dimen", "android")
        val topBound = if (statusBarRes > 0) resources.getDimensionPixelSize(statusBarRes) else 0
        val bottomBound = screenH
        val gap = dp(8)

        // 4) 竖直：优先下方 -> 上方 -> 兜底，最后钳制在可见区
        val petLeft = petParams.x
        val petTop = petParams.y
        val belowTop = petTop + petH + gap
        val aboveTop = petTop - menuH - gap
        val desiredY = when {
            belowTop + menuH <= bottomBound -> belowTop
            aboveTop >= topBound -> aboveTop
            else -> petTop + gap
        }
        val maxY = maxOf(topBound, bottomBound - menuH)
        val menuY = desiredY.coerceIn(topBound, maxY)

        // 5) 水平：以小猫为中心，按实测宽度钳制到屏幕内
        val desiredX = petLeft + (petW - menuW) / 2
        val maxX = maxOf(0, screenW - menuW)
        val menuX = desiredX.coerceIn(0, maxX)

        // 6) 应用位置并添加窗口（gravity 仍为 TOP|START，x/y 即左上角）
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = menuX
            y = menuY
        }

        try {
            wm.addView(menu, params)
            menuView = menu
        } catch (e: Exception) {
            menuView = null
        }
    }