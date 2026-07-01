# Fragment To Compose Template

```kotlin
class ExampleFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?) =
        ComposeView(requireContext()).apply {
            setContent { ExampleRoute() }
        }
}
```
