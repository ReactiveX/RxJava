# Contributing to RxJava 3.x

If you would like to contribute code you can do so through GitHub by forking the repository and sending a pull request targeting the branch `3.x`.

When submitting code, please make every effort to follow existing conventions and style in order to keep the code as readable as possible.

## AI contributions

We are not against contributions from AI tools, LLM-based or future architectures. However, you as a human are responsible for its contributions and suggestions.

This means, you have to make sure it doesn't hallucinate issues or elements of the contribution, doesn't try to hack rewards or hack established unit tests, doesn't go wild
and rearchitect established components.

If you post a contribution that is broken, we will not argue with your LLM or prompt engineer for you. You are responsible for having the the LLM's output work within the confines
of this project.

Please also be aware that this project is large both in current and historical sense with some rules not documented or enforced by unit tests. This is because such unwritten rules
were trivial or readily inferrable by humans in the past. The project predates LLMs several years and thus is not organized to be accessible by LLMs today. Nor should it be.

Consequently, the amount of prompting and the context window size to include all possible information about it could become so much that it can become prohibitively expensive to have
an LLM come up with more than basic and trivial contributions. Needless to say, don't bankrupt yourself and don't just accept the LLM's output at face value.

## License

By contributing your code, you agree to license your contribution under the terms of the APLv2: https://github.com/ReactiveX/RxJava/blob/3.x/LICENSE

All files are released with the Apache 2.0 license.

If you are adding a new file it should have a header like this:

```
/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
 ```
