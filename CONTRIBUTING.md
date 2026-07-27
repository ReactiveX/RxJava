# Contributing to RxJava 4.x

If you would like to contribute code you can do so through GitHub by forking the repository and sending a pull request targeting the branch `4.x`.

If necessary, we can decide if your contribution can or needs to be backported to fix a major issue with the previous maintained version.

:warning: Do not target 3.x with new operators, bugfixes, cosmetic or pedantic changes.

When submitting code, please make every effort to follow existing conventions and style in order to keep the code as readable as possible.

When you contribute, consider any amplification of your work towards us. We don't have the capacity to propagate your changes to relevant other places in the code. So if you fix `Observable`, there is a good chance `Flowable` and `Streamable` may need changes too. You can do this in multiple PRs over a bigger timespan.

We'd also like to avoid low-effort drive by PRs, spam, changes that clearly show you are trying to prop up your repertoire with a popular repository.

:warning: In addition, failing to respond to change requests, clarification requests or detailed reasons for a change will result in your PR being rejected on principle, even if technically correct.

:information_source: If you are/were using AI to manipulate the code, we require you disclose it in your PR text. If you discussed your change with an AI but coded it yourself, we'd still like you disclosed that and a brief except of your and the AIs reasoning for the change.

:stop_sign: Due to the maturity of the project (10-14 years depending on where you count it started) and in an age of low effort, AI laden content creation, we are taking a much harsher stance of you causing us trouble with your conduct: you will be banned and the issue/PR title changed to `[Banned, reason]` for everyone to see. If you hide your public contributions in your profile and contribute to us, no matter what you did, you will be automatically banned. 

**Transparency over one's conduct is a primary principle in this project.**

## AI contributions

We are not against contributions from AI tools, LLM-based or future architectures. However, you as a human are responsible for its contributions and suggestions.

This means, you have to make sure it doesn't hallucinate issues or elements of the contribution, doesn't try to hack rewards or hack established unit tests, doesn't go wild
and rearchitect established components.

If you post a contribution that is broken, we will not argue with your LLM or prompt engineer for you. You are responsible for having the LLM's output work within the confines
of this project.

Please also be aware that this project is large both in current and historical sense with some rules not documented or enforced by unit tests. This is because such unwritten rules
were trivial or readily inferrable by humans in the past. The project predates LLMs several years and thus is not organized to be accessible by LLMs today. Nor should it be.

Consequently, the amount of prompting and the context window size to include all possible information about it could become so much that it can become prohibitively expensive to have
an LLM come up with more than basic and trivial contributions. Needless to say, don't bankrupt yourself and don't just accept the LLM's output at face value.

## License

By contributing your code, you agree to license your contribution under the terms of the APLv2: https://github.com/ReactiveX/RxJava/blob/4.x/LICENSE

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
