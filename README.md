# crawler
using RabbitMQ

Робот для сбора публикаций с новостного сайта. (С использованием многопоточности и брокера сообщений RabbitMQ)

Сначала происходит скачивание главной страницы в один поток, из нее парсятся все ссылки и добавляются в очередь LINKS RabbitMQ. Далее несколько потоков достают ссылки из очереди, качают соответствующие станицы и кладут их в другую очередь PAGES. Затем несколько потоков достают страницы из очереди PAGES, парсят заголовок, дату публикации, текст, преобразовывают эти данные в формат json и кладут в третью очередь CONTENTS.
