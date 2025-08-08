using WebApplication3.Models;

namespace WebApplication3.ViewModels
{
    public class TextbookModel: Textbook
    {
        public TextbookModel() 
        {
            textBookList = new List <Textbook>();
        }
        public List<Textbook> textBookList { get; set; }
    }
}
