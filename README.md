Задание команды №10 на Blockchain Hackathon 48h

Задача: Реализовать сервис обеспечитального залога (на любом блокчейн).

Решение:
- Залогодатель создаёт и подписывает транзакцию на перевод n-суммы токенов на адрес общего пула отчуждённых залогов, транзакция при этом имеет статус замороженной
- Транзакция помещается в общий пул транзакций и остаётся  там n-количество блоков	
- Если один из участников сети обнаруживает противоправное действие со стороны залогодателя, то транзакция с его депозитом помечается как активная и рассылается далее по сети, остальные участники, в свою очередь, подтверждают или не подтверждают факт противоправного действия, принимая или отвергая транзакцию на списание депозита со счёта залогодателя
- Если наибольшее число узлов сети подтверждают транзакцию на отчуждение залога, соответствующая сумма попадает в общий пул отчуждённых транзакций
- В случае, если противоправных действий за время эмиссии  n-количества блоков не обнаружено, блок попадает в чейн с пометкой замороженного


Для реализации прототипа сервиса использована самописная имплементация блокчейн-клиента (Kotlin (JVM), Gradle)
