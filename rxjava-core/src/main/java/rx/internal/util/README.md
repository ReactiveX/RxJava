This `rx.internal.*` package is for internal use only. Any code here can change at any time and is not considered part of the public API, even if the classes are `public` so as to be used from other packages within `rx.*`.

If you depend on these classes, your code may break in any future RxJava release, even if it's just a patch release (major.minor.patch).
