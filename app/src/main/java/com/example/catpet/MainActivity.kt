<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <!-- 标题 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:paddingTop="8dp"
            android:paddingBottom="8dp"
            android:text="@string/app_name"
            android:textSize="22sp"
            android:textStyle="bold" />

        <!-- 今日待办统计占位卡片 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:background="@color/ic_launcher_background"
            android:orientation="vertical"
            android:padding="16dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/entry_todo"
                android:textSize="16sp"
                android:textStyle="bold" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:text="@string/card_todo_placeholder"
                android:textSize="14sp" />
        </LinearLayout>

        <!-- 定时提醒状态占位卡片 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:background="@color/ic_launcher_background"
            android:orientation="vertical"
            android:padding="16dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/entry_reminder"
                android:textSize="16sp"
                android:textStyle="bold" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:text="@string/card_reminder_placeholder"
                android:textSize="14sp" />
        </LinearLayout>

        <!-- 小猫控制卡片 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:background="@color/ic_launcher_background"
            android:orientation="vertical"
            android:padding="16dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/cat_card_title"
                android:textSize="16sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvStatus"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:text="@string/status_ready"
                android:textSize="14sp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:orientation="horizontal">

                <Button
                    android:id="@+id/btnStart"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/btn_start" />

                <Button
                    android:id="@+id/btnStop"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="8dp"
                    android:layout_weight="1"
                    android:text="@string/btn_stop" />
            </LinearLayout>
        </LinearLayout>

        <!-- 六入口：第 1 行 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:orientation="horizontal">

            <Button
                android:id="@+id/btnTodo"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/entry_todo" />

            <Button
                android:id="@+id/btnReminder"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:layout_weight="1"
                android:text="@string/entry_reminder" />
        </LinearLayout>

        <!-- 六入口：第 2 行 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal">

            <Button
                android:id="@+id/btnAi"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/entry_ai" />

            <Button
                android:id="@+id/btnNotes"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:layout_weight="1"
                android:text="@string/entry_notes" />
        </LinearLayout>

        <!-- 六入口：第 3 行 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:layout_marginBottom="16dp"
            android:orientation="horizontal">

            <Button
                android:id="@+id/btnGithub"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/entry_github" />

            <Button
                android:id="@+id/btnSettings"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:layout_weight="1"
                android:text="@string/entry_settings" />
        </LinearLayout>

    </LinearLayout>
</ScrollView>