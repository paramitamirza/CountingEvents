[SemEval-2018 Task 5: Counting Events and Participants in the Long Tail](https://competitions.codalab.org/competitions/17285)

The data for this task consists of three directories:
1) input (which contains the input for the systems, namely: the questions in a JSON format and the corresponding documents in CoNLL)
2) example_submission (which contains an example submission by a system)
3) dev_data (which contains the gold standard data for the trial task)

The gold standard data in the dev_data directory is the same as the gold standard data in the codalab competition online. To evaluate a submission, you can directly use the scoring functionality of the codalab task online.


The content of the submission directory has to be archived in a .zip format prior to its uploading in codalab. This can be done by executing the following commands in your terminal:

cd example_submission
zip -r submission.zip *

You can find detailed information about the evaluation and the answer formats at the codalab competition page.

Organizers and Contact:
* Marten Postma (m.c.postma@vu.nl)
* Filip Ilievski (f.ilievski@vu.nl)
* Piek Vossen (piek.vossen@vu.nl)

