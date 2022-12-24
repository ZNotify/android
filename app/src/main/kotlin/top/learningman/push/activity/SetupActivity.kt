package top.learningman.push.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.setupwizardlib.SetupWizardLayout
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch
import top.learningman.push.data.Repo
import top.learningman.push.databinding.ActivitySetupBinding
import top.learningman.push.provider.AutoChannel
import top.learningman.push.provider.Permission
import top.learningman.push.provider.WebSocket
import top.learningman.push.utils.PermissionManager
import top.learningman.push.utils.setTextAnimation
import com.android.setupwizardlib.R as SuwR

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var setup: SetupWizardLayout
    private lateinit var adapter: Adapter
    private lateinit var title: MaterialTextView
    private var isGrantPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action == PERMISSION_GRANT_ACTION) {
            isGrantPermission = true
        }

        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setup = binding.setup
        title = binding.setup.findViewById(SuwR.id.suw_layout_title)!!


        with(window) {
            WindowCompat.setDecorFitsSystemWindows(this, false)
        }

        adapter = Adapter(this)

        viewPager = binding.viewPager
        viewPager.adapter = adapter

        viewPager.setPageTransformer { view, position ->
            view.apply {
                val pageWidth = width
                when {
                    -1 <= position && position <= 1 -> {
                        translationX = pageWidth * -position
                    }
                }
                alpha = when {
                    position < -1 -> {
                        0f
                    }
                    position <= 1 -> {
                        1 - kotlin.math.abs(position)
                    }
                    else -> {
                        0f
                    }
                }
            }
        }
        viewPager.isUserInputEnabled = false
        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (position == adapter.pages.size - 1) {
                        setup.navigationBar.nextButton.text = "完成"
                    }

                    val fragment = adapter.pages[position]
                    if (fragment is FragmentWithTitle) {
                        title.setTextAnimation(fragment.title)
                    }
                }
            })

        viewPager.offscreenPageLimit = 1
        if (isGrantPermission) {
            viewPager.setCurrentItem(adapter.itemCount - 1, false)
        }

        setup = binding.setup
        setup.navigationBar.backButton.visibility = View.GONE
        setup.navigationBar.nextButton.setOnClickListener {
            if (viewPager.currentItem == adapter.itemCount - 1) {
                if (isGrantPermission) {
                    finish()
                } else {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    finish()
                }
            } else {
                viewPager.currentItem += 1
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val statusBarHeight = WindowInsetsCompat.toWindowInsetsCompat(
            window.decorView.rootWindowInsets,
            window.decorView
        ).getInsets(WindowInsetsCompat.Type.statusBars()).top
        val navigationBarHeight = WindowInsetsCompat.toWindowInsetsCompat(
            window.decorView.rootWindowInsets,
            window.decorView
        ).getInsets(WindowInsetsCompat.Type.navigationBars())

        title.setPadding(
            title.paddingLeft,
            statusBarHeight + title.paddingTop,
            title.paddingRight,
            title.paddingBottom
        )

        setup.setPadding(
            0,
            0,
            0,
            navigationBarHeight.bottom
        )
    }

    private class Adapter(fa: SetupActivity) :
        FragmentStateAdapter(fa) {
        val pages: MutableList<Fragment> = mutableListOf()

        init {
            pages += WelcomeFragment()
            pages += CurrentFragment()
            pages += RequestPermissionFragment()
        }

        override fun getItemCount(): Int {
            return pages.size
        }

        override fun createFragment(position: Int): Fragment {
            return pages[position]
        }
    }

    fun setNextButtonEnabled(enabled: Boolean) {
        setup.navigationBar.nextButton.isEnabled = enabled
    }

    interface FragmentWithTitle {
        val title: String
    }

    class WelcomeFragment : Fragment(), FragmentWithTitle {
        override val title = "欢迎"

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            return ComposeView(requireContext()).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnLifecycleDestroyed(
                        viewLifecycleOwner
                    )
                )
                setContent {
                    MaterialTheme {
                        Column(modifier = Modifier.padding(40.dp)) {
                            Text("欢迎使用 Notify，Notify 是一个通过推送服务来提醒你的应用。")
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Notify 接受 POST/PUT 发送的请求，并将结果推送到你的设备上")
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Notify 当前仅支持自动选择推送服务")
                            Text("在您的设备支持多个推送服务时，Notify 将会自动选择一个。")
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("如果您的设备不支持任何推送服务，Notify 将会启用自身的长连接推送实现，但是这将会消耗更多的电量。")
                        }
                    }
                }
            }
        }
    }

    class CurrentFragment : Fragment(), FragmentWithTitle {
        override val title = "当前推送服务"

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val channel = AutoChannel.getInstance(requireContext())
            with(Repo.getInstance(requireContext())) {
                setChannel(channel.name)
            }
            return ComposeView(requireContext()).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnLifecycleDestroyed(
                        viewLifecycleOwner
                    )
                )
                setContent {
                    MaterialTheme {
                        Column(modifier = Modifier.padding(40.dp)) {
                            Text("当前启用的推送服务是： ${channel.name}")

                            if (AutoChannel.by(WebSocket)) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text("当前推送实现会消耗额外的电量。", color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            Text("接下来，请为 Notify 授予必要的权限。")
                        }
                    }
                }
            }
        }
    }

    class RequestPermissionFragment : Fragment(),
        FragmentWithTitle {
        override val title = "授权"
        private lateinit var manager: PermissionManager

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            manager = PermissionManager(requireActivity())
            val ps = manager.permissions.map {
                it to it.check(requireContext())
            }.toTypedArray()
            val permissions = mutableStateListOf(
                *ps
            )
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    permissions.replaceAll {
                        val check = it.first.check(requireContext())
                        if (check != it.second) {
                            it.first to check
                        } else {
                            it
                        }
                    }
                }
            }

            return ComposeView(requireContext()).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
                )
                setContent {
                    MaterialTheme {
                        Column(modifier = Modifier.padding(40.dp)) {
                            Text(text = "应用需要以下权限以运行：")
                            Spacer(modifier = Modifier.height(16.dp))
                            permissions.map {
                                PermissionLayout(permission = it.first, pass = it.second)
                            }
                        }
                    }
                }
            }
        }

        @Composable
        fun PermissionLayout(permission: Permission, pass: Boolean?) {
            Column {
                Text(text = permission.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    enabled = pass != true,
                    onClick = { permission.grant(requireActivity()) }) {
                    Text(text = if (pass == true) "已授权" else "授权")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }


        override fun onResume() {
            super.onResume()
            view?.requestLayout()
            (requireActivity() as SetupActivity).setNextButtonEnabled(manager.ok())
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!isGrantPermission) {
            super.onBackPressed()
        }
    }

    companion object {
        const val PERMISSION_GRANT_ACTION = "permission_grant_action"
    }
}